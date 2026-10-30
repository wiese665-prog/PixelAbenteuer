package de.pixelabenteuer.game;
/** One frame resolver for rendering, foot anchoring and equipment attachment. */
final class CharacterPose {
 static final float FOOT_OFFSET=24f,WORLD_SIZE=80.64f;
 static final class Frame {final String page;final int row,col;Frame(String p,int r,int c){page=p;row=r;col=c;}}
 static int facing(int dir){return dir==0?0:dir==1?1:dir==2?3:2;}
 static Frame frame(int role,boolean creator,int facing,String action,float time,float elapsed,int sequence){
  if(creator)return new Frame(role==1?"pBOW":role==2?"pCAST":"pONE2",facing,0);
  if(role>0&&(action.equals("hurt")||action.equals("death")))action=action.equals("death")&&time>=.12f&&time<.24f?"crouch":"idle";
  boolean attack=action.equals("attack"),moving=action.equals("walk")||action.equals("run"),fighting=attack||action.equals("hurt")||action.equals("death");
  if(role>0)return new Frame(attack?(role==1?"pBOW":"pCAST"):"p1",moving?4+facing:facing,action.equals("crouch")?1:attack?Math.min(3,Math.max(0,(int)(time*10))):moving?Math.floorMod((int)(elapsed*(action.equals("run")?12:8)),6):0);
  return new Frame(fighting?"pONE3":"p1",fighting?(attack?(sequence%2)*4+facing:action.equals("hurt")?facing:4+facing):moving?4+facing:facing,fighting?Math.min(3,Math.max(0,(int)(time*10))):moving?Math.floorMod((int)(elapsed*(action.equals("run")?12:8)),6):0);
 }
 static float centerY(float footY,int nativeBottom,float size){return footY-(nativeBottom-32)*size/64f;}
 static int layerSample(int value,int pivot,float scale){return Math.max(0,Math.min(127,(int)Math.floor(pivot+(value-pivot)/scale)));}
 static String shield(boolean iron,int facing){return facing>=2?(iron?"shP3":"shP1"):(iron?"sh03":"sh01");}
}
