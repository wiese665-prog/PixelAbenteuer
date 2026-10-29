"""Review-only asymmetric inn, original sprite pixels and original palette at 1:1."""
from pathlib import Path
from PIL import Image,ImageDraw
R=Path(__file__).resolve().parents[2];A=R/'app/src/main/assets';O=Path(__file__).resolve().parent
forge=Image.open(A/'sprites/Exterior_house.png').convert('RGBA')
forge.putalpha(forge.getchannel('A').point(lambda a:255 if a>=128 else 0))
out=Image.new('RGBA',(224,176))
# A low, separate guest wing with its own whole native gabled roof.
out.alpha_composite(forge.crop((148,120,188,160)),(20,125))
out.alpha_composite(forge.crop((152,24,216,74)),(8,78))
# A short covered stone passage joins the annex to the main building.
for x in [58,70]:out.alpha_composite(forge.crop((150,120,162,160)),(x,125))
out.alpha_composite(forge.crop((160,116,184,120)),(58,123))
# The main two-storey timber hall retains whole roof, facade, windows and door.
main=forge.crop((0,0,145,163));d=ImageDraw.Draw(main)
d.rectangle((0,119,16,147),fill=(0,0,0,0)) # remove the smithy's shield sign
out.alpha_composite(main,(64,2))
# Tavern sign, deliberately drawn on the same 1px grid with sampled source colors.
wood=[p for p in forge.crop((150,90,160,110)).get_flattened_data() if p[3]]
dark=min(wood,key=lambda p:sum(p[:3]));light=max(wood,key=lambda p:sum(p[:3]));brown=forge.getpixel((150,94));cream=max((p for p in forge.crop((150,122,250,150)).get_flattened_data() if p[3] and p[0]>=p[1]>=p[2]),key=lambda p:sum(p[:3]))
d=ImageDraw.Draw(out);d.rectangle((64,119,78,120),fill=dark);d.rectangle((65,121,66,126),fill=dark);d.rectangle((61,127,73,141),fill=dark);d.rectangle((62,128,72,140),fill=brown);d.rectangle((63,129,71,130),fill=light);d.rectangle((64,132,67,137),fill=cream);d.rectangle((68,133,69,136),fill=cream);d.point((68,135),fill=brown)
assert {p for p in out.get_flattened_data() if p[3]}<={p for p in forge.get_flattened_data() if p[3]}
out.save(O/'inn-v3-native.png')
preview=Image.new('RGBA',out.size,'#759e53');preview.alpha_composite(out);preview.resize((896,704),Image.Resampling.NEAREST).save(O/'inn-v3-review.png')
print('New asymmetric inn proposal: source pixels at 1:1, source palette only, no game replacement.')
