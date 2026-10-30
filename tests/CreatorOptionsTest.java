package de.pixelabenteuer.game;
import java.util.Arrays;
final class CreatorOptionsTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args){
  int[] styles={4,3,0};String[] names={"Magier","Bogenschütze","Ritter"};
  for(int i=0;i<3;i++){
   int style=styles[i];check(CreatorOptions.className(style).equals(names[i]),"class label");
   check(CreatorOptions.cycleClass(style,1)==styles[(i+1)%3],"forward cycle");
   check(CreatorOptions.cycleClass(style,-1)==styles[(i+2)%3],"backward cycle");
   for(int wardrobe=0;wardrobe<6;wardrobe++){
    String[] clothes=CreatorOptions.clothes(wardrobe);String[] weapons=CreatorOptions.weapons(style);
    check(weapons.length==(style==0?2:1),"class weapon kit");
    check(Arrays.equals(clothes,CreatorOptions.clothes(wardrobe)),"wardrobe changed by class");
    check(CharacterEquipment.outfit(CreatorOptions.chest(wardrobe),0)!=null,"preview chest");
    check(CharacterEquipment.hat(CreatorOptions.hat(wardrobe))!=null,"preview hat");
    if(clothes.length>0){check(clothes[0].equals(CreatorOptions.chest(wardrobe)),"spawn chest differs from preview");check(clothes[1].equals(CreatorOptions.hat(wardrobe)),"spawn hat differs from preview");clothes[0]="modified";check(!CreatorOptions.chest(wardrobe).equals("modified"),"mutable kit");}
   }
  }
  check(CreatorOptions.normalizeStyle(1)==0&&CreatorOptions.normalizeStyle(2)==0,"legacy axe/mace migration");
  check(CreatorOptions.normalizeStyle(3)==3&&CreatorOptions.normalizeStyle(4)==4,"saved ranged classes");
  for(String hat:new String[]{"hood","irHl","wdHl","leHl"})check(CreatorOptions.coversHair(hat),"hair leaks through "+hat);
  check(!CreatorOptions.coversHair("")&&!CreatorOptions.coversHair("mgHt"),"uncovered hair/mage changed");
  check(CreatorOptions.clothing(-1)==5&&CreatorOptions.clothing(6)==0,"wardrobe wrap");
  check(CharacterPose.frame(1,true,0,"idle",0,0,0).page.equals("pBOW"),"bow not shown in armed preview");
  check(CharacterPose.frame(2,true,0,"idle",0,0,0).page.equals("pCAST"),"staff not shown in armed preview");
  float scale=CharacterPose.WORLD_SIZE/64f;
  check(CharacterPose.layerSample(64,64,scale)==64&&CharacterPose.layerSample(96,96,scale)==96,"foot anchor drift");
  for(int pixel=0;pixel<128;pixel++)check(CharacterPose.layerSample(pixel,64,1)==pixel,"native pixel changed");
  check(CharacterPose.layerSample(72,64,scale)==70,"occlusion x scale");
  check(CharacterPose.layerSample(48,96,scale)==57,"occlusion y scale");
  System.out.println("Three independent classes, 18 wardrobe combinations, legacy styles, hair coverage and native raster coordinates passed.");
 }
}
