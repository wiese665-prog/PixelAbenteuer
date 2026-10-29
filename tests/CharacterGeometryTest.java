package de.pixelabenteuer.game;
import java.io.File;
import java.util.HashMap;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
public class CharacterGeometryTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  HashMap<String,BufferedImage> bodies=new HashMap<>();
  HashMap<String,Integer> feet=new HashMap<>();
  for(int role=0;role<3;role++)for(int facing=0;facing<4;facing++)for(String action:new String[]{"idle","walk","run","attack","hurt","death","crouch"})for(int tick=0;tick<40;tick++){
   CharacterPose.Frame f=CharacterPose.frame(role,false,facing,action,tick*.03f,tick*.13f,tick%2);
   check(f.row>=0&&f.row<8&&f.col>=0&&f.col<8,"frame outside sheet");
   String key=f.page+":"+f.row+":"+f.col;
   Integer bottom=feet.get(key);
   if(bottom==null){
    BufferedImage body=bodies.get(f.page);
    if(body==null){body=ImageIO.read(new File("app/src/main/assets/sprites/ManaSeedCharacter/char_a_"+f.page+"/char_a_"+f.page+"_0bas_humn_v01.png"));bodies.put(f.page,body);}
    bottom=0;outer:for(int yy=63;yy>=0;yy--)for(int xx=0;xx<64;xx++)if((body.getRGB(f.col*64+xx,f.row*64+yy)>>>24)>=128){bottom=yy+1;break outer;}
    check(bottom>0,"empty body pose");feet.put(key,bottom);
   }
   float footY=524,center=CharacterPose.centerY(footY,bottom,CharacterPose.WORLD_SIZE);
   // World ground is projected around the same foot anchor as the upright sprite.
   float spriteFoot=.82f*(footY-500)+(center-footY)+(bottom-32)*CharacterPose.WORLD_SIZE/64;
   float shadowFoot=.82f*(footY-500);
   check(Math.abs(spriteFoot-shadowFoot)<.001,"rendered feet detached from projected shadow");
  }
  check(CharacterPose.frame(1,false,3,"attack",.2f,0,0).page.equals("pBOW"),"archer attack page");
  check(CharacterPose.frame(2,false,2,"attack",.2f,0,0).page.equals("pCAST"),"mage attack page");
  check(CharacterPose.frame(2,false,0,"death",.18f,0,0).col==1,"crouch transition");
  int[] creatorDirections={0,3,1,2},expectedFacing={0,2,1,3};
  for(int i=0;i<4;i++)check(CharacterPose.facing(creatorDirections[i])==expectedFacing[i],"creator direction mapping");
  for(boolean iron:new boolean[]{false,true})for(int facing:new int[]{2,3}){
   String name=CharacterPose.shield(iron,facing);
   BufferedImage shield=ImageIO.read(new File("app/src/main/assets/sprites/ManaSeedCharacter/char_a_pONE2/7tlb/char_a_pONE2_7tlb_"+name+"_v01.png"));
   int left=64,right=-1;for(int yy=0;yy<64;yy++)for(int xx=0;xx<64;xx++)if((shield.getRGB(xx,facing*64+yy)>>>24)>=128){left=Math.min(left,xx);right=Math.max(right,xx);}
   check(right>=left&&right-left+1<=8,"renderer resolved a frontal shield instead of a profile");
  }
  check(CharacterPose.shield(false,3).equals("shP1")&&CharacterPose.shield(true,2).equals("shP3"),"side shield resolver");
  for(int y=334;y<=470;y+=8){check(VillageSafety.protectedArea(VillageSafety.TOWER_X,y),"tower path unsafe");check(VillageSafety.wildlifeExcluded(VillageSafety.TOWER_X+48,y),"animals too close to tower path");}
  for(int y=96;y<=890;y+=12)for(int x=100;x<=1230;x+=12)if(VillageSafety.protectedArea(x,y))check(VillageSafety.wildlifeExcluded(x+48,y)&&VillageSafety.wildlifeExcluded(x-48,y)&&VillageSafety.wildlifeExcluded(x,y+48)&&VillageSafety.wildlifeExcluded(x,y-48),"missing attack clearance");
  check(!VillageSafety.wildlifeExcluded(1500,750),"forest gameplay excluded");
  System.out.println("Character geometry: shared frames, exact feet, side shields, town entrances and wildlife buffer passed.");
 }
}
