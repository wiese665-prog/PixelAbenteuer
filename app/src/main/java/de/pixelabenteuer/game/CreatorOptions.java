package de.pixelabenteuer.game;

/** Independent class/weapon and wardrobe choices. Legacy combat IDs stay stable. */
final class CreatorOptions {
 private static final int[] STYLES={4,3,0};
 private static final String[] LABELS={"Magier","Bogenschütze","Ritter"};
 private static final String[] CLOTHES={"Unterkleidung","Holzrüstung","Lederrüstung","Eisenrüstung","Waldläuferkleidung","Magierkleidung"};
 private static final String[][] KITS={{},{"Wooden Armor","Wooden Helmet"},{"Leather Armor","Leather Helmet"},{"Iron Armor","Iron Helmet"},{"Waldlaeuferwams","Waldlaeuferkapuze"},{"Magierrobe","Magierhut"}};
 static int normalizeStyle(int style){return style==3||style==4?style:0;}
 static int classIndex(int style){return style==4?0:style==3?1:2;}
 static String className(int style){return LABELS[classIndex(style)];}
 static int cycleClass(int style,int delta){return STYLES[Math.floorMod(classIndex(style)+delta,STYLES.length)];}
 static int clothing(int value){return Math.floorMod(value,CLOTHES.length);}
 static String clothingName(int value){return CLOTHES[clothing(value)];}
 static String[] clothes(int value){return KITS[clothing(value)].clone();}
 static String chest(int value){String[] k=KITS[clothing(value)];return k.length==0?null:k[0];}
 static String hat(int value){String[] k=KITS[clothing(value)];return k.length==0?null:k[1];}
 static String[] weapons(int style){return style==4?new String[]{"Magierstab"}:style==3?new String[]{"Jagdbogen"}:new String[]{"Wooden Sword","Wooden Shield"};}
 static boolean coversHair(String hat){return "hood".equals(hat)||"irHl".equals(hat)||"wdHl".equals(hat)||"leHl".equals(hat)||"pfht".equals(hat);}
 private CreatorOptions(){}
}
