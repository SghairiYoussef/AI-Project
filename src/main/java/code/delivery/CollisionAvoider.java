package code.delivery;

import code.model.Position;
import java.util.*;

public class CollisionAvoider {
    public static Map<String,List<Position>> makeSafe(Map<String,List<Position>> routes){
        Map<String,List<Position>> sched = new LinkedHashMap<>();
        for(Map.Entry<String,List<Position>> e : routes.entrySet()) sched.put(e.getKey(), new ArrayList<>(e.getValue()));

        boolean changed=true; int pass=0;
        while(changed && pass<2000){
            changed=false;
            Map<Integer, Map<Position,String>> reserved = new HashMap<>();
            int maxT=0;
            for(List<Position> r: sched.values()) if(r!=null) maxT=Math.max(maxT, r.size());
            for(int t=0;t<maxT;t++){
                for(String id: new ArrayList<>(sched.keySet())){
                    List<Position> r = sched.get(id);
                    if(r==null || r.isEmpty()) continue;
                    Position p = (t<r.size()) ? r.get(t) : r.get(r.size()-1);
                    Map<Position,String> atT = reserved.computeIfAbsent(t, k->new HashMap<>());
                    if(atT.containsKey(p)){
                        int insert = Math.min(t, r.size()-1);
                        Position prev = r.get(Math.max(0, insert-1));
                        r.add(insert, prev);
                        changed=true;
                        break;
                    } else atT.put(p, id);
                }
                if(changed) break;
            }
            pass++;
        }
        return sched;
    }
}
