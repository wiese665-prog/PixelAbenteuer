package de.pixelabenteuer.game;
/** Deterministic per-object idle periods: no synchronized ambient animation loops. */
public final class AmbientMotion {
 private AmbientMotion(){}
 public static int frame(float time,float x,float y,int frames,float fps,float minRest,int restVariation){
  if(frames<=1)return 0;
  int seed=Math.round(x*31)^Math.round(y*17)^(frames*7919);
  float duration=frames/fps,period=duration+minRest+Math.floorMod(seed,Math.max(1,restVariation));
  float offset=Math.floorMod(seed,997)/997f*period;
  float phase=(time+offset)%period;
  return phase<duration?Math.min(frames-1,(int)(phase*fps)):0;
 }
}
