package solim.performance;

import arc.util.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;

/**
 * Writes the self-contained 3-tab interactive profile report.
 *
 * <p>
 * The report is a single HTML file with no external network dependencies: all
 * CSS and JavaScript
 * are inlined and the whole profile is embedded as one JSON payload. Three
 * views are provided:
 * <ol>
 * <li><b>Flame Graph</b> — the unified {@code build} / {@code signal} /
 * {@code computed} /
 * {@code effect} / {@code flush} timeline, zoomable and pannable, with search
 * filtering and a
 * click-to-inspect details panel. The Chrome Trace and Speedscope payloads
 * remain embedded so the
 * session can still be opened in external tools.</li>
 * <li><b>Component Tree</b> — the live hierarchy from {@link SessionRoot} with
 * an expandable tree on
 * the left and a reactive inspector for the selected component on the
 * right.</li>
 * <li><b>Reactive Hotspots</b> — sortable tables ranking signals, computeds,
 * and effects.</li>
 * </ol>
 *
 * <p>
 * All payload strings are escaped before being embedded, so component names,
 * signal values, and
 * dependency labels containing quotes or angle brackets cannot break out of the
 * JSON script block.
 */
public final class ProfileReportExporter {

    private ProfileReportExporter() {
    }

    /**
     * Renders the report as an HTML document.
     *
     * @param spans        recorded trace spans for the session
     * @param droppedCount number of spans evicted by the ring buffer
     * @param traceId      the trace generation id
     * @param components   live component tree snapshot
     */
    public static String render(@Nullable List<TraceSpan> spans, int droppedCount, long traceId,
            @Nullable List<ComponentNodeSnapshot> components) {
        List<TraceSpan> ordered = FlameExporter.ordered(spans);
        String payload = payload(ordered, droppedCount, traceId, components);
        String chrome = FlameExporter.chromePayload(ordered, 0.0, droppedCount, traceId);
        String speedscope = FlameExporter.speedscopePayload(ordered, 0.0, droppedCount, traceId);

        StringBuilder sb = new StringBuilder(96 * 1024);
        sb.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">");
        sb.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
        sb.append("<title>Solim Profile Report</title>");
        sb.append("<style>");
        sb.append(stylesheet());
        sb.append("</style></head><body>");
        sb.append(header(ordered.size(), droppedCount, traceId));
        sb.append(nav());
        sb.append("<main>");
        sb.append(flameTab());
        sb.append(treeTab());
        sb.append(hotspotsTab());
        sb.append("</main>");
        sb.append("<script type=\"application/json\" id=\"solim-profile\">");
        sb.append(Json.escapeScriptBlock(payload));
        sb.append("</script>");
        sb.append("<script type=\"application/json\" id=\"solim-chrome\">");
        sb.append(Json.escapeScriptBlock(chrome));
        sb.append("</script>");
        sb.append("<script type=\"application/json\" id=\"solim-speedscope\">");
        sb.append(Json.escapeScriptBlock(speedscope));
        sb.append("</script>");
        sb.append("<script>");
        sb.append(script());
        sb.append("</script>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Builds the single JSON payload consumed by the report's three views.
     *
     * <p>
     * Exposed separately from {@link #render} so tests can assert on the embedded
     * data without
     * parsing the surrounding HTML.
     */
    public static String payload(@Nullable List<TraceSpan> spans, int droppedCount, long traceId,
            @Nullable List<ComponentNodeSnapshot> components) {
        StringBuilder sb = new StringBuilder(8 * 1024);
        sb.append("{\"traceId\":").append(traceId);
        sb.append(",\"droppedCount\":").append(Math.max(0, droppedCount));
        sb.append(",\"spans\":[");
        boolean first = true;
        for (TraceSpan span : spans != null ? spans : Collections.<TraceSpan>emptyList()) {
            if (span == null) {
                continue;
            }
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"spanId\":").append(span.spanId);
            sb.append(",\"parentId\":").append(span.parentId);
            sb.append(",\"depth\":").append(span.depth);
            sb.append(",\"name\":\"").append(Json.escape(span.name)).append('"');
            sb.append(",\"phase\":\"").append(Json.escape(span.phase)).append('"');
            sb.append(",\"startNs\":").append(span.startNs);
            sb.append(",\"endNs\":").append(span.endNs);
            sb.append(",\"durationNs\":").append(span.durationNs());
            sb.append(",\"childCount\":").append(span.childCount).append('}');
        }
        sb.append("],\"components\":");
        appendComponents(sb, components);
        sb.append('}');
        return sb.toString();
    }

    private static void appendComponents(StringBuilder sb, @Nullable List<ComponentNodeSnapshot> components) {
        if (components == null || components.isEmpty()) {
            sb.append("[]");
            return;
        }
        sb.append('[');
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            appendComponent(sb, components.get(i));
        }
        sb.append(']');
    }

    private static void appendComponent(StringBuilder sb, ComponentNodeSnapshot node) {
        sb.append("{\"id\":").append(node.id);
        sb.append(",\"className\":\"").append(Json.escape(node.className)).append('"');
        sb.append(",\"name\":\"").append(Json.escape(node.name)).append('"');
        sb.append(",\"elementType\":").append(node.elementType == null
                ? "null"
                : "\"" + Json.escape(node.elementType) + "\"");
        sb.append(",\"bounds\":[").append(node.bounds[0]).append(',').append(node.bounds[1])
                .append(',').append(node.bounds[2]).append(',').append(node.bounds[3]).append(']');
        sb.append(",\"visible\":").append(node.visible);
        sb.append(",\"disposed\":").append(node.disposed);
        sb.append(",\"children\":");
        appendComponents(sb, node.children);
        sb.append(",\"signals\":[");
        for (int i = 0; i < node.signals.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            SignalSnapshot signal = node.signals.get(i);
            sb.append("{\"id\":").append(signal.id);
            sb.append(",\"value\":\"").append(Json.escape(signal.value)).append('"');
            sb.append(",\"writeCount\":").append(signal.writeCount);
            sb.append(",\"observerCount\":").append(signal.observerCount);
            sb.append(",\"listenerCount\":").append(signal.listenerCount).append('}');
        }
        sb.append("],\"computeds\":[");
        for (int i = 0; i < node.computeds.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            ComputedSnapshot computed = node.computeds.get(i);
            sb.append("{\"id\":").append(computed.id);
            sb.append(",\"value\":\"").append(Json.escape(computed.value)).append('"');
            sb.append(",\"recomputeCount\":").append(computed.recomputeCount);
            sb.append(",\"totalNs\":").append(computed.totalNs);
            sb.append(",\"lastNs\":").append(computed.lastNs);
            sb.append(",\"dirty\":").append(computed.dirty);
            appendStringArray(sb, computed.dependencies);
            sb.append('}');
        }
        sb.append("],\"effects\":[");
        for (int i = 0; i < node.effects.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            EffectSnapshot effect = node.effects.get(i);
            sb.append("{\"id\":").append(effect.id);
            sb.append(",\"runCount\":").append(effect.runCount);
            sb.append(",\"totalNs\":").append(effect.totalNs);
            sb.append(",\"lastNs\":").append(effect.lastNs);
            appendStringArray(sb, effect.dependencies);
            sb.append('}');
        }
        sb.append("],\"otherDisposables\":[");
        for (int i = 0; i < node.otherDisposables.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(Json.escape(node.otherDisposables.get(i))).append('"');
        }
        sb.append("]}");
    }

    private static void appendStringArray(StringBuilder sb, List<String> values) {
        sb.append(",\"dependencies\":[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(Json.escape(values.get(i))).append('"');
        }
        sb.append(']');
    }

    public static void writeFile(@Nullable List<TraceSpan> spans, int droppedCount, long traceId,
            @Nullable List<ComponentNodeSnapshot> components, @Nullable File file) throws IOException {
        if (file == null) {
            throw new IOException("Profile report target file cannot be null");
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create directories for " + file);
        }
        String html = render(spans, droppedCount, traceId, components);
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        try {
            writer.write(html);
        } finally {
            try {
                writer.close();
            } catch (IOException ignored) {

            }
        }
    }

    private static String header(int spanCount, int droppedCount, long traceId) {
        return "<header class=\"bar\"><h1>Solim Profile Report</h1><div class=\"meta\">"
                + "<span>spans <b>" + spanCount + "</b></span>"
                + "<span>dropped <b>" + Math.max(0, droppedCount) + "</b></span>"
                + "<span>trace <b>" + traceId + "</b></span>"
                + "</div></header>";
    }

    private static String nav() {
        return "<nav class=\"tabs\" role=\"tablist\">"
                + "<button class=\"tab active\" data-tab=\"flame\" role=\"tab\">Flame Graph</button>"
                + "<button class=\"tab\" data-tab=\"tree\" role=\"tab\">Component Tree</button>"
                + "<button class=\"tab\" data-tab=\"hotspots\" role=\"tab\">Reactive Hotspots</button>"
                + "</nav>";
    }

    private static String flameTab() {
        return "<section class=\"tabpanel active\" id=\"tab-flame\">"
                + "<div class=\"toolbar\">"
                + "<input type=\"search\" id=\"flame-search\" placeholder=\"Filter spans by name…\">"
                + "<label class=\"field\">min ms <input type=\"number\" id=\"flame-min\" value=\"0\" min=\"0\" step=\"0.05\"></label>"
                + "<button id=\"flame-reset\">Reset zoom</button>"
                + "<span class=\"legend\">"
                + "<i class=\"swatch build\"></i>build"
                + "<i class=\"swatch signal\"></i>signal"
                + "<i class=\"swatch computed\"></i>computed"
                + "<i class=\"swatch effect\"></i>effect"
                + "<i class=\"swatch flush\"></i>flush"
                + "</span>"
                + "</div>"
                + "<div id=\"flame-viewport\"><canvas id=\"flame-canvas\"></canvas></div>"
                + "<aside id=\"flame-detail\" class=\"detail\"><p class=\"muted\">Select a span to inspect it.</p></aside>"
                + "</section>";
    }

    private static String treeTab() {
        return "<section class=\"tabpanel\" id=\"tab-tree\">"
                + "<div class=\"split\">"
                + "<div class=\"pane\"><div class=\"toolbar\">"
                + "<input type=\"search\" id=\"tree-search\" placeholder=\"Filter components…\">"
                + "<span class=\"muted\" id=\"tree-count\"></span>"
                + "</div><div id=\"tree-view\" class=\"tree\"></div></div>"
                + "<div class=\"pane\" id=\"inspector\"><p class=\"muted\">Select a component to inspect it.</p></div>"
                + "</div></section>";
    }

    private static String hotspotsTab() {
        return "<section class=\"tabpanel\" id=\"tab-hotspots\">"
                + "<h2>Signals by write count</h2><div id=\"hot-signals\"></div>"
                + "<h2>Computed by total time</h2><div id=\"hot-computeds\"></div>"
                + "<h2>Effects by total time</h2><div id=\"hot-effects\"></div>"
                + "</section>";
    }

    private static String stylesheet() {
        return ":root{--bg:#0f1115;--panel:#171a21;--line:#2a2f3a;--fg:#e6e8ee;--muted:#8b93a7;--accent:#5b9dff}"
                + "*{box-sizing:border-box}"
                + "body{margin:0;background:var(--bg);color:var(--fg);font:14px/1.45 system-ui,Segoe UI,Roboto,sans-serif}"
                + ".bar{display:flex;align-items:baseline;gap:20px;padding:14px 18px;border-bottom:1px solid var(--line)}"
                + "h1{font-size:16px;margin:0}h2{font-size:14px;margin:18px 0 8px;color:var(--muted);text-transform:uppercase;letter-spacing:.06em}"
                + ".meta{display:flex;gap:16px;color:var(--muted);font-size:12px}"
                + ".tabs{display:flex;gap:4px;padding:10px 18px;border-bottom:1px solid var(--line)}"
                + ".tab{background:transparent;border:1px solid var(--line);color:var(--muted);padding:6px 14px;border-radius:6px;cursor:pointer}"
                + ".tab.active{color:var(--fg);border-color:var(--accent);background:#1d2330}"
                + ".tabpanel{display:none;padding:14px 18px}.tabpanel.active{display:block}"
                + ".toolbar{display:flex;gap:12px;align-items:center;flex-wrap:wrap;margin-bottom:10px}"
                + "input[type=search],input[type=number]{background:var(--panel);border:1px solid var(--line);color:var(--fg);padding:6px 10px;border-radius:6px}"
                + "input[type=search]{min-width:240px}input[type=number]{width:80px}"
                + "button{background:var(--panel);border:1px solid var(--line);color:var(--fg);padding:6px 12px;border-radius:6px;cursor:pointer}"
                + ".field{color:var(--muted);font-size:12px;display:flex;align-items:center;gap:6px}"
                + ".legend{display:flex;align-items:center;gap:10px;margin-left:auto;color:var(--muted);font-size:12px}"
                + ".swatch{width:11px;height:11px;border-radius:2px;display:inline-block;margin-left:8px}"
                + ".swatch.build{background:#4f7dd6}.swatch.signal{background:#d98a3c}.swatch.computed{background:#4bb37b}"
                + ".swatch.effect{background:#c05fd6}.swatch.flush{background:#5b5fd6}"
                + "#flame-viewport{position:relative;overflow:hidden;border:1px solid var(--line);border-radius:8px;background:#12151b;cursor:grab}"
                + "#flame-viewport.panning{cursor:grabbing}"
                + "#flame-canvas{display:block}"
                + ".detail{margin-top:10px;padding:10px 12px;border:1px solid var(--line);border-radius:8px;background:var(--panel);min-height:74px}"
                + ".detail dl{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:6px 18px;margin:0}"
                + ".detail dt{color:var(--muted);font-size:11px;text-transform:uppercase;letter-spacing:.05em}"
                + ".detail dd{margin:0;font-variant-numeric:tabular-nums}"
                + ".split{display:grid;grid-template-columns:minmax(240px,1fr) minmax(280px,1.4fr);gap:14px}"
                + ".pane{border:1px solid var(--line);border-radius:8px;background:var(--panel);padding:12px;max-height:70vh;overflow:auto}"
                + ".tree-row{display:flex;align-items:center;gap:6px;padding:3px 4px;border-radius:4px;cursor:pointer}"
                + ".tree-row:hover{background:#1d2330}.tree-row.selected{background:#243049}"
                + ".tree-toggle{width:14px;color:var(--muted);text-align:center;user-select:none}"
                + ".tree-name{font-variant-numeric:tabular-nums}.tree-badge{color:var(--muted);font-size:11px;margin-left:auto;padding-left:8px}"
                + ".muted{color:var(--muted)}.mono{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:12px}"
                + "table{border-collapse:collapse;width:100%;margin-bottom:8px}"
                + "th,td{text-align:left;padding:5px 8px;border-bottom:1px solid var(--line);font-size:12px}"
                + "th{color:var(--muted);cursor:pointer;user-select:none;text-transform:uppercase;letter-spacing:.05em;font-size:11px}"
                + "td.num,th.num{text-align:right;font-variant-numeric:tabular-nums}"
                + "tr:hover td{background:#1d2330}"
                + ".empty{color:var(--muted);padding:10px 2px}";
    }

    private static String script() {
        return String.join("\n",
                "(function(){",
                "'use strict';",
                "var payload=JSON.parse(document.getElementById('solim-profile').textContent);",
                "var PHASES=['build','signal','computed','effect','flush'];",
                "var COLORS={build:'#4f7dd6',signal:'#d98a3c',computed:'#4bb37b',effect:'#c05fd6',flush:'#5b5fd6'};",
                "function el(t,c){var n=document.createElement(t);if(c)n.className=c;return n;}",
                "function txt(v){return v===null||v===undefined?'':String(v);}",
                "function ns(v){return (v||0)/1e6;}",

                "Array.prototype.forEach.call(document.querySelectorAll('.tab'),function(btn){",
                "btn.addEventListener('click',function(){",
                "Array.prototype.forEach.call(document.querySelectorAll('.tab'),function(b){b.classList.remove('active');});",
                "Array.prototype.forEach.call(document.querySelectorAll('.tabpanel'),function(p){p.classList.remove('active');});",
                "btn.classList.add('active');",
                "document.getElementById('tab-'+btn.dataset.tab).classList.add('active');",
                "if(btn.dataset.tab==='flame')drawFlame();",
                "});",
                "});",

                "var ROW=20,PAD=4;",
                "var view={start:0,end:1,filter:'',minMs:0,selected:null};",
                "var canvas=document.getElementById('flame-canvas'),ctx=canvas.getContext('2d');",
                "var viewport=document.getElementById('flame-viewport');",
                "function visibleSpans(){",
                "var q=view.filter.toLowerCase();",
                "return (payload.spans||[]).filter(function(s){",
                "return ns(s.durationNs)>=view.minMs&&(!q||s.name.toLowerCase().indexOf(q)>=0);",
                "});",
                "}",
                "function timeRange(){",
                "var spans=payload.spans||[];",
                "if(!spans.length)return{lo:0,hi:1};",
                "var lo=Infinity,hi=-Infinity;",
                "spans.forEach(function(s){if(s.startNs<lo)lo=s.startNs;if(s.endNs>hi)hi=s.endNs;});",
                "return{lo:lo,hi:hi>lo?hi:lo+1};",
                "}",
                "function resetZoom(){var r=timeRange();view.start=r.lo;view.end=r.hi;}",
                "function drawFlame(){",
                "var spans=visibleSpans(),r=timeRange();",
                "var maxDepth=0;spans.forEach(function(s){if(s.depth>maxDepth)maxDepth=s.depth;});",
                "var width=viewport.clientWidth||900;",
                "var height=Math.max(60,(maxDepth+1)*ROW+PAD*2);",
                "var dpr=window.devicePixelRatio||1;",
                "canvas.style.width=width+'px';canvas.style.height=height+'px';",
                "canvas.width=width*dpr;canvas.height=height*dpr;",
                "ctx.setTransform(dpr,0,0,dpr,0,0);ctx.clearRect(0,0,width,height);",
                "if(!spans.length){ctx.fillStyle='#8b93a7';ctx.font='13px system-ui';ctx.fillText('No spans recorded.',12,24);return;}",
                "var span=view.end-view.start||1;",
                "spans.forEach(function(s){",
                "var left=(s.startNs-view.start)/span*width;",
                "var w=Math.max(1,(s.endNs-s.startNs)/span*width);",
                "var top=s.depth*ROW+PAD;",
                "ctx.fillStyle=COLORS[s.phase]||'#4a4f5c';",
                "ctx.fillRect(left,top,w,ROW-3);",
                "if(s.spanId===view.selected){ctx.strokeStyle='#fff';ctx.lineWidth=2;ctx.strokeRect(left,top,w,ROW-3);}",
                "if(w>34){ctx.fillStyle='rgba(255,255,255,.92)';ctx.font='11px system-ui';",
                "ctx.save();ctx.beginPath();ctx.rect(left,top,w,ROW-3);ctx.clip();",
                "ctx.fillText(s.name,left+4,top+12);ctx.restore();}",
                "});",
                "}",
                "function hitTest(x,y){",
                "var spans=visibleSpans(),r=timeRange();",
                "var span=view.end-view.start||1;",
                "var width=viewport.clientWidth||900;",
                "for(var i=spans.length-1;i>=0;i--){",
                "var s=spans[i];var left=(s.startNs-view.start)/span*width;",
                "var w=Math.max(1,(s.endNs-s.startNs)/span*width);var top=s.depth*ROW+PAD;",
                "if(x>=left&&x<=left+w&&y>=top&&y<=top+ROW-3)return s;",
                "}",
                "return null;",
                "}",
                "function chainOf(span){",
                "var byId={},out=[];",
                "(payload.spans||[]).forEach(function(s){byId[s.spanId]=s;});",
                "var cur=span;while(cur){out.unshift(cur.name);cur=byId[cur.parentId];}",
                "return out.join(' › ');",
                "}",
                "function showDetail(span){",
                "var box=document.getElementById('flame-detail');box.innerHTML='';",
                "if(!span){box.innerHTML='<p class=\\\"muted\\\">Select a span to inspect it.</p>';return;}",
                "var dl=el('dl');",
                "[['Name',span.name],['Phase',span.phase],['Total (ms)',ns(span.durationNs).toFixed(3)],",
                "['Depth',span.depth],['Children',span.childCount],['Span / parent',span.spanId+' / '+span.parentId]]",
                ".forEach(function(pair){var dt=el('dt');dt.textContent=pair[0];var dd=el('dd');dd.textContent=pair[1];dl.appendChild(dt);dl.appendChild(dd);});",
                "box.appendChild(dl);",
                "var p=el('p','muted mono');p.textContent=chainOf(span);box.appendChild(p);",
                "}",
                "viewport.addEventListener('click',function(ev){",
                "var rect=canvas.getBoundingClientRect();",
                "var hit=hitTest(ev.clientX-rect.left,ev.clientY-rect.top);",
                "view.selected=hit?hit.spanId:null;drawFlame();showDetail(hit);",
                "});",
                "var drag=null;",
                "viewport.addEventListener('mousedown',function(ev){drag={x:ev.clientX,s:view.start,e:view.end};viewport.classList.add('panning');});",
                "window.addEventListener('mouseup',function(){drag=null;viewport.classList.remove('panning');});",
                "window.addEventListener('mousemove',function(ev){",
                "if(!drag)return;",
                "var r=timeRange(),width=viewport.clientWidth||900;",
                "var per=((drag.e-drag.s)/width)*(ev.clientX-drag.x);",
                "var lo=Math.max(r.lo,Math.min(drag.s+per,drag.e-1)),hi=Math.min(r.hi,Math.max(drag.e+per,drag.s+1));",
                "view.start=lo;view.end=hi;drawFlame();",
                "});",
                "viewport.addEventListener('wheel',function(ev){",
                "ev.preventDefault();",
                "var rect=canvas.getBoundingClientRect();",
                "var width=viewport.clientWidth||900;",
                "var f=((ev.clientX-rect.left)/width);",
                "var span=view.end-view.start;var at=view.start+f*span;var next=span*(ev.deltaY>0?1.25:0.8);",
                "view.start=at-f*next;view.end=view.start+next;drawFlame();",
                "},{passive:false});",
                "document.getElementById('flame-search').addEventListener('input',function(ev){view.filter=ev.target.value;drawFlame();});",
                "document.getElementById('flame-min').addEventListener('input',function(ev){view.minMs=parseFloat(ev.target.value)||0;drawFlame();});",
                "document.getElementById('flame-reset').addEventListener('click',function(){resetZoom();drawFlame();});",
                "window.addEventListener('resize',function(){if(document.getElementById('tab-flame').classList.contains('active'))drawFlame();});",

                "var selectedId=null;",
                "function badge(n,s,c,e){",
                "var out=[];if(n)out.push(n+' child');if(s)out.push(s+' sig');if(c)out.push(c+' comp');if(e)out.push(e+' fx');",
                "return out.join(' · ');",
                "}",
                "function renderTree(filter){",
                "var q=(filter||'').toLowerCase();",
                "var view2=document.getElementById('tree-view');view2.innerHTML='';",
                "var shown=0;",
                "function matches(node){",
                "if(node.name.toLowerCase().indexOf(q)>=0||node.className.toLowerCase().indexOf(q)>=0)return true;",
                "return (node.children||[]).some(matches);",
                "}",
                "function add(node,parent,depth){",
                "if(q&&!matches(node))return;",
                "shown++;",
                "var row=el('div','tree-row'+(node.id===selectedId?' selected':''));",
                "row.style.paddingLeft=(depth*14)+'px';",
                "var kids=node.children||[];",
                "var toggle=el('span','tree-toggle');",
                "toggle.textContent=kids.length?(row.dataset.open==='0'?'▸':'▾'):'';",
                "row.appendChild(toggle);",
                "var name=el('span','tree-name');name.textContent=node.name;row.appendChild(name);",
                "var badge2=el('span','tree-badge');",
                "badge2.textContent=badge(kids.length,(node.signals||[]).length,(node.computeds||[]).length,(node.effects||[]).length);",
                "row.appendChild(badge2);",
                "row.addEventListener('click',function(ev){",
                "if(ev.target===toggle&&kids.length){row.dataset.open=row.dataset.open==='0'?'1':'0';renderTree(filter);return;}",
                "selectedId=node.id;renderTree(filter);renderInspector(node);",
                "});",
                "parent.appendChild(row);",
                "if(kids.length&&row.dataset.open!=='0')kids.forEach(function(k){add(k,parent,depth+1);});",
                "}",
                "var roots=payload.components||[];",
                "if(!roots.length){view2.innerHTML='<p class=\\\"muted\\\">No live components captured.</p>';}",
                "roots.forEach(function(r){add(r,view2,0);});",
                "document.getElementById('tree-count').textContent=shown+' shown';",
                "}",
                "function table(rows,columns){",
                "if(!rows.length)return '<p class=\\\"empty\\\">Nothing recorded.</p>';",
                "var head='<tr>'+columns.map(function(c){return '<th class=\"'+(c.num?'num':'')+'\">'+c.label+'</th>';}).join('')+'</tr>';",
                "var body=rows.map(function(r){return '<tr>'+columns.map(function(c){",
                "var v=r[c.key];return '<td class=\"'+(c.num?'num':'')+'\">'+(v===undefined||v===null?'':String(v))+'</td>';",
                "}).join('')+'</tr>';}).join('');",
                "return '<table>'+head+body+'</table>';",
                "}",
                "function renderInspector(node){",
                "var box=document.getElementById('inspector');box.innerHTML='';",
                "var h=el('h2');h.textContent=node.name;box.appendChild(h);",
                "var meta=el('p','muted mono');",
                "var b=(node.bounds||[]).map(function(v){return Math.round(v);});",
                "meta.textContent='class='+node.className+' element='+(node.elementType||'not built')+' bounds=['+b.join(', ')+']'+' visible='+node.visible+' disposed='+node.disposed;",
                "box.appendChild(meta);",
                "box.appendChild(section('Signals',table(node.signals||[],[",
                "{key:'id',label:'#',num:true},{key:'value',label:'Value'},{key:'writeCount',label:'Writes',num:true},",
                "{key:'observerCount',label:'Observers',num:true},{key:'listenerCount',label:'Listeners',num:true}])));",
                "box.appendChild(section('Computed',table(node.computeds||[],[",
                "{key:'id',label:'#',num:true},{key:'value',label:'Value'},{key:'recomputeCount',label:'Recomputes',num:true},",
                "{key:'totalNs',label:'Total (ms)',num:true},{key:'lastNs',label:'Last (ms)',num:true},",
                "{key:'dirty',label:'Dirty'},{key:'deps',label:'Deps',num:true}])));",
                "box.appendChild(section('Effects',table(node.effects||[],[",
                "{key:'id',label:'#',num:true},{key:'runCount',label:'Runs',num:true},",
                "{key:'totalNs',label:'Total (ms)',num:true},{key:'lastNs',label:'Last (ms)',num:true},",
                "{key:'deps',label:'Deps',num:true}])));",
                "if((node.otherDisposables||[]).length){",
                "var p=el('p','muted');p.textContent='Other disposables: '+node.otherDisposables.join(', ');box.appendChild(p);",
                "}",
                "}",
                "function section(title,html){",
                "var wrap=el('div');var h=el('h2');h.textContent=title;wrap.appendChild(h);",
                "wrap.insertAdjacentHTML('beforeend',html);return wrap;",
                "}",
                "document.getElementById('tree-search').addEventListener('input',function(ev){renderTree(ev.target.value);});",

                "function collect(list,node,out){",
                "(list||[]).forEach(function(n){out.push(n);});",
                "(node.children||[]).forEach(function(k){collect(list,k,out);});",
                "}",
                "function gather(key){",
                "var out=[];",
                "function walk(node){",
                "collect(node[key],node,out);",
                "(node.children||[]).forEach(walk);",
                "}",
                "(payload.components||[]).forEach(walk);",
                "var seen={},uniq=[];",
                "out.forEach(function(n){if(seen[n.id])return;seen[n.id]=1;uniq.push(n);});",
                "return uniq;",
                "}",
                "function withDeps(nodes){",
                "return nodes.map(function(n){",
                "return Object.assign({},n,{deps:(n.dependencies||[]).length,totalMs:ns(n.totalNs),lastMs:ns(n.lastNs),avgMs:n.totalNs?ns(n.totalNs)/(n.runCount||n.recomputeCount||1):0});",
                "});",
                "}",
                "function renderHotspots(){",
                "var signals=gather('signals');",
                "signals.sort(function(a,b){return b.writeCount-a.writeCount;});",
                "document.getElementById('hot-signals').innerHTML=table(signals,[",
                "{key:'id',label:'#',num:true},{key:'value',label:'Value'},{key:'writeCount',label:'Writes',num:true},",
                "{key:'observerCount',label:'Observers',num:true},{key:'listenerCount',label:'Listeners',num:true}]);",
                "var computeds=withDeps(gather('computeds'));",
                "computeds.sort(function(a,b){return b.totalMs-a.totalMs;});",
                "document.getElementById('hot-computeds').innerHTML=table(computeds,[",
                "{key:'id',label:'#',num:true},{key:'value',label:'Value'},{key:'recomputeCount',label:'Recomputes',num:true},",
                "{key:'totalMs',label:'Total (ms)',num:true},{key:'avgMs',label:'Avg (ms)',num:true},",
                "{key:'lastMs',label:'Last (ms)',num:true},{key:'dirty',label:'Dirty'},{key:'deps',label:'Deps',num:true}]);",
                "var effects=withDeps(gather('effects'));",
                "effects.sort(function(a,b){return b.totalMs-a.totalMs;});",
                "document.getElementById('hot-effects').innerHTML=table(effects,[",
                "{key:'id',label:'#',num:true},{key:'runCount',label:'Runs',num:true},",
                "{key:'totalMs',label:'Total (ms)',num:true},{key:'avgMs',label:'Avg (ms)',num:true},",
                "{key:'lastMs',label:'Last (ms)',num:true},{key:'deps',label:'Deps',num:true}]);",
                "}",

                "document.addEventListener('click',function(ev){",
                "var th=ev.target;if(th.tagName!=='TH')return;",
                "var table2=th.closest('table');if(!table2)return;",
                "var idx=Array.prototype.indexOf.call(th.parentNode.children,th);",
                "var asc=th.dataset.dir!=='asc';th.dataset.dir=asc?'asc':'desc';",
                "Array.prototype.forEach.call(table2.querySelectorAll('tbody tr,tr'),function(){});",
                "var rows=Array.prototype.slice.call(table2.querySelectorAll('tr')).slice(1);",
                "rows.sort(function(a,b){",
                "var av=a.children[idx]?a.children[idx].textContent:'';var bv=b.children[idx]?b.children[idx].textContent:'';",
                "var an=parseFloat(av),bn=parseFloat(bv);",
                "var cmp=(!isNaN(an)&&!isNaN(bn))?an-bn:av.localeCompare(bv);",
                "return asc?cmp:-cmp;",
                "});",
                "rows.forEach(function(r){table2.appendChild(r);});",
                "});",
                "window.solimProfile={payload:payload};",
                "resetZoom();drawFlame();renderTree('');renderHotspots();",
                "})();");
    }

    static final class Json {
        private Json() {
        }

        static String escape(@Nullable String value) {
            if (value == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder(value.length() + 16);
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '"') {
                    sb.append("\\\"");
                } else if (c == '\\') {
                    sb.append("\\\\");
                } else if (c == '\n') {
                    sb.append("\\n");
                } else if (c == '\r') {
                    sb.append("\\r");
                } else if (c == '\t') {
                    sb.append("\\t");
                } else if (c == '<') {

                    sb.append("\\u003c");
                } else if (c == '>') {
                    sb.append("\\u003e");
                } else if (c < 0x20) {
                    sb.append(String.format("\\u%04x", (int) c));
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        static String escapeScriptBlock(@Nullable String json) {
            if (json == null) {
                return "{}";
            }
            return json.replace("</", "<\\/");
        }
    }
}
