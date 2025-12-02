package code.viz;

import code.model.*;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Swing visualizer: sequential replay and simultaneous animation.
 */
public class SwingVisualizer extends JPanel {
    private final Grid grid;
    private final Map<String, Color> agentColor = new HashMap<>();
    private final Map<String, List<Position>> routes = new LinkedHashMap<>();
    private final Map<String, Integer> indices = new HashMap<>();
    private final int cellSize;

    public SwingVisualizer(Grid g){
        this.grid = g;
        int maxDim = Math.max(g.width, g.height);
        this.cellSize = Math.max(20, 700 / maxDim);
        setPreferredSize(new Dimension(g.width * cellSize, g.height * cellSize));
        Color[] palette = {Color.RED, Color.BLUE, Color.MAGENTA, Color.ORANGE, Color.CYAN, Color.GREEN.darker(), Color.PINK, Color.YELLOW, Color.GRAY};
        int i = 0;
        for(Agent a : g.agents){ agentColor.put(a.id, palette[i % palette.length]); i++; }
    }

    public void setRoute(Agent agent, List<Position> route){
        routes.put(agent.id, route);
        indices.put(agent.id, 0);
    }

    public void startSimultaneous(int delayMs){
        Timer timer = new Timer(delayMs, e -> {
            boolean any = false;
            for(String id : new ArrayList<>(routes.keySet())){
                int idx = indices.getOrDefault(id, 0);
                List<Position> r = routes.get(id);
                if(idx < r.size()-1){ indices.put(id, idx+1); any = true; }
            }
            repaint();
            if(!any) ((Timer)e.getSource()).stop();
        });
        timer.start();
    }

    public void startSequential(int delayMs){
        List<String> order = new ArrayList<>(routes.keySet());
        Timer[] tRef = new Timer[1];
        tRef[0] = new Timer(delayMs, e -> {
            boolean allDone = true;
            for(String id : order){
                int idx = indices.getOrDefault(id, 0);
                List<Position> r = routes.get(id);
                if(idx < r.size()-1){
                    indices.put(id, idx+1);
                    repaint();
                    allDone = false;
                    break; // animate only one agent at a time
                }
            }
            if(allDone) tRef[0].stop();
        });
        tRef[0].start();
    }

    @Override protected void paintComponent(Graphics g0){
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // grid cells
        for(int y=0;y<grid.height;y++){
            for(int x=0;x<grid.width;x++){
                int sx = x*cellSize, sy = y*cellSize;
                Position p = new Position(x,y);
                if(grid.stores.contains(p)) g.setColor(new Color(80,130,230));
                else if(grid.destinations.contains(p)) g.setColor(new Color(60,200,100));
                else g.setColor(Color.WHITE);
                g.fillRect(sx, sy, cellSize, cellSize);
                g.setColor(Color.LIGHT_GRAY);
                g.drawRect(sx, sy, cellSize, cellSize);
            }
        }

        // blocked edges
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(3f));
        Map<String,Integer> edges = grid.getEdgeCostMap();
        for(Map.Entry<String,Integer> e : edges.entrySet()){
            if(e.getValue() == 0){
                String k = e.getKey();
                String[] parts = k.split(":");
                String[] a = parts[0].split(",");
                String[] b = parts[1].split(",");
                int x1 = Integer.parseInt(a[0]), y1 = Integer.parseInt(a[1]);
                int x2 = Integer.parseInt(b[0]), y2 = Integer.parseInt(b[1]);
                int cx1 = x1*cellSize + cellSize/2, cy1 = y1*cellSize + cellSize/2;
                int cx2 = x2*cellSize + cellSize/2, cy2 = y2*cellSize + cellSize/2;
                g.drawLine(cx1, cy1, cx2, cy2);
            }
        }

        // tunnels dashed
        g.setColor(Color.DARK_GRAY);
        float[] dash = {6f};
        for(Position[] t : grid.getTunnels()){
            int cx1 = t[0].x*cellSize + cellSize/2, cy1 = t[0].y*cellSize + cellSize/2;
            int cx2 = t[1].x*cellSize + cellSize/2, cy2 = t[1].y*cellSize + cellSize/2;
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, dash, 0f));
            g.drawLine(cx1, cy1, cx2, cy2);
        }

        // agents
        for(Agent a : grid.agents){
            List<Position> r = routes.get(a.id);
            int idx = indices.getOrDefault(a.id, 0);
            Position cur;
            if(r==null || r.isEmpty()) cur = a.pos;
            else cur = r.get(Math.min(idx, r.size()-1));
            Color col = agentColor.getOrDefault(a.id, Color.RED);
            g.setColor(col);
            int rx = cur.x * cellSize + Math.max(6, cellSize/8), ry = cur.y * cellSize + Math.max(6, cellSize/8);
            g.fillOval(rx, ry, Math.max(8, cellSize - 2*(cellSize/8)), Math.max(8, cellSize - 2*(cellSize/8)));
            g.setColor(Color.WHITE);
            g.drawString(a.id, rx+4, ry+12);
        }
    }

    public static void showFrame(Grid grid, Map<Agent, List<Position>> plannedRoutes, int delayMs, boolean sequential){
        SwingVisualizer vis = new SwingVisualizer(grid);
        for(Map.Entry<Agent,List<Position>> e : plannedRoutes.entrySet()) vis.setRoute(e.getKey(), new ArrayList<>(e.getValue()));
        // compute conflict-free schedules
        Map<String, List<Position>> sched = vis.buildConflictFreeSchedules();
        Map<Agent, List<Position>> finalMap = new LinkedHashMap<>();
        for(Agent a : grid.agents){
            List<Position> r = sched.get(a.id);
            if(r==null) r = plannedRoutes.getOrDefault(a, Collections.singletonList(a.pos));
            finalMap.put(a, r);
        }

        JFrame f = new JFrame("Delivery Visualizer");
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.add(vis);
        for(Map.Entry<Agent,List<Position>> e : finalMap.entrySet()) vis.setRoute(e.getKey(), e.getValue());
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        if(sequential) vis.startSequential(delayMs);
        else vis.startSimultaneous(delayMs);
    }

    // schedule builder copied to be accessible here
    public Map<String, List<Position>> buildConflictFreeSchedules(){
        Map<String, List<Position>> sched = new LinkedHashMap<>();
        for(String id : routes.keySet()){
            List<Position> r = routes.get(id);
            if(r == null) r = new ArrayList<>();
            sched.put(id, new ArrayList<>(r));
        }
        // ensure non-empty
        for(String id : sched.keySet()){
            List<Position> r = sched.get(id);
            if(r==null || r.isEmpty()){
                Position start = null;
                for(Agent a : grid.agents) if(a.id.equals(id)) start = a.pos;
                if(start != null){ r = new ArrayList<>(); r.add(start); sched.put(id, r); }
            }
        }
        Map<Integer, Map<Position, String>> reserved = new HashMap<>();
        boolean changed = true; int pass=0;
        while(changed && pass<500){
            changed = false; reserved.clear();
            int maxT = 0;
            for(List<Position> r : sched.values()) maxT = Math.max(maxT, r.size());
            for(int t=0;t<maxT;t++){
                for(String aid : new ArrayList<>(sched.keySet())){
                    List<Position> r = sched.get(aid);
                    Position p = (t < r.size()) ? r.get(t) : r.get(r.size()-1);
                    Map<Position,String> atT = reserved.computeIfAbsent(t, k->new HashMap<>());
                    if(atT.containsKey(p)){
                        int insertPos = Math.min(t, r.size()-1);
                        Position prev = r.get(Math.max(0, insertPos-1));
                        r.add(insertPos, prev);
                        changed = true;
                        break;
                    } else {
                        atT.put(p, aid);
                    }
                }
                if(changed) break;
            }
            pass++;
        }
        return sched;
    }
}
