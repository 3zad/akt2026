# Jabur jadatöötluskeel Jada

Jada on täisarvuliste massiivide omistamise ja itereerimise keel, kus väärtustamise (täpsemalt täitmise) tulemuseks on uus väärtuskeskkond.
Massiivide pikkused on ette teada ja omistatav massiiv määrab ette ära, kui pikki massiive kasutatakse omistamise parema poole avaldises.
Eeldame, et programmis arvutatakse massiividega, mille pikkused sobivad kokku, st. vigu ei pea tuvastama.
Ainsaks erandiks on literaalid, mis tuleb venitada sobiva pikkusega massiivideks (nagu [NumPy _broadcasting_](https://numpy.org/doc/stable/user/basics.broadcasting.html)).
Näiteks, kui muutuja `x` väärtus on `[11, 22, 33]`, siis:
* omistamises `x <- x + 1` venitatakse arvliteraal konstantseks jadaks `[1, 1, 1]` ja tulemuses on muutuja `x` väärtus `[12, 23, 34]`;
* omistamises `x <- x + [0..]` venitatakse kasvava jada literaal jadaks `[0, 1, 2]` ja tulemuses on muutuja `x` väärtus `[11, 23, 35]`.

Järgmine programm illustreerib keele kõige olulisemat funktsionaalsust:
```
{ 
  arr <- [0..];
  barr <- arr + 1;
  z <- 0;
  for i in barr do 
    z <- z + i
}
```
Kui `arr` ja `barr` on mõlemad 3-elemendilised massiivid ning `z` ja `i` on mõlemad 1-elemendilised massiivid, siis selle näite käivitamise tulemuseks on keskkond
```
{arr → [0, 1, 2]; barr → [1, 2, 3]; z → [6]; i → [3]}
```

Lisaks on keeles võimalik lugeda ja kirjutada massiivide elemente indeksi kaudu.
Näiteks omistamine `arr @ 1 <- barr_2` asendab `arr` massiivi 2. elemendi `barr` massiivi 3. elemendiga, mille Java analoog on `arr[1] = barr[2]`.

Keele AST klassid paiknevad _eksam2.ast_ paketis ja nende ülemklassiks on _JadaNode_:

* _JadaExpr_ — avaldised, alamklassidega:
    * _JadaNum_ — arvliteraal/konstantse jada literaal;
    * _JadaFrom_ — kasvava jada literaal;
    * _JadaVar_ — muutuja (võib-olla indeksiga);
    * _JadaAdd_ — liitmine;
* _JadaStmt_ — laused, alamklassidega:
    * _JadaAssign_ — omistamine (võib-olla indeksiga);
    * _JadaBlock_ — lausete plokk;
    * _JadaForeach_ — itereerimine.

Klassis _JadaNode_ on staatilised abimeetodid, millega saab mugavamalt abstraktseid süntaksipuid luua.
Ülalolev lause moodustatakse järgmiselt:
```
block(
  assign("arr", from(0)), 
  assign("barr", add(var("arr"), num(1))), 
  foreach("i", "barr", 
    assign("z", add(var("z"), var("i")))
  )
)
```


## Alusosa: JadaEvaluator

Klassis _JadaEvaluator_ tuleb implementeerida meetod _eval_, mis täidab lause etteantud väärtuskeskkonnas.
Väärtustamisele ja täitmisele kehtivad järgmised nõuded:

1. Konstantse jada literaali väärtuseks on sobiva pikkusega massiiv, mis sisaldab literaali väärtuseid.
2. Kasvava jada literaali väärtuseks on sobiva pikkusega massiiv, mis algab literaali väärtusega ja suureneb igal indeksil ühe võrra.
3. Ilma indeksita muutuja väärtuseks on antud massiiv keskkonnast. Võib eeldada, et see on sobiva pikkusega.
4. Indeksiga muutuja väärtuseks on 1-elemendiline massiiv, mis sisaldab antud massiivi antud indeksil olevat väärtust. Indeksiks on samuti 1-elemendiline massiiv.
5. Liitmise tulemuseks on massiiv, mis saadakse argumentmassiive indekshaaval kokku liites. Võib eeldada, et argumentmassiivid on sama pikkusega.
6. Etteantud lause täitmise tulemuseks on uus väärtuskeskkond, mitte üksik väärtus.
7. Ilma indeksita omistamine omistab terve massiivi. Omistatava massiivi pikkus määrab _sobiva pikkuse_ avaldiste väärtustamiseks.
8. Indeksiga omistamine omistab massiivi antud indeksile. Sel juhul on avaldiste väärtustamiseks _sobiv pikkus_ 1.
9. Plokk käitub standardselt.
10. Itereerimine toimub samuti standardselt ja on analoogiline Java `for` (täpsemalt _for-each_) lausega. Võib eeldada, et itereerimise keha ise itereerimist ei sisalda.
11. Võib eeldada, et defineerimata muutujaid ei kasutata.


## Põhiosa: JadaAst

Failis _Jada.g4_ tuleb implementeerida grammatika ja klassis _JadaAst_ tuleb implementeerida meetod _parseTreeToAst_, mis teisendab parsepuu AST-iks.
Süntaksile kehtivad järgmised nõuded:

1. Arvliteraal/konstantse jada literaal koosneb numbritest, millele võib eelneda miinusmärk. Esimene number tohib olla 0 ainult siis, kui see on arvu ainuke number.
2. Kasvava jada literaal koosneb avavast haaksulust (`[`), arvuliteraalist, kahest punktist (`..`) ja lõpetavast haaksulust (`]`).
3. Muutuja koosneb vähemalt ühest ladina tähest (suured ja väiksed).
4. Muutujale võib järgneda alakriips (`_`) ja indekseerimise avaldis.
5. Ainsaks binaarseks operaatoriks on liitmine (`+`), mis on vasakassotsiatiivne.
6. Indekseerimine on kõrgema prioriteediga kui liitmine.
7. Avaldistes võib kasutada sulge, mis on kõige kõrgema prioriteediga.
8. Omistamine koosneb muutuja nimest (millele võib järgneda `@` ja indekseerimise avaldis), noolest (`<-`) ja avaldisest.
9. Plokk koosneb avavast loogelisest sulust (`{`), semikooloniga eraldatud lausete jadast ja lõpetavast loogelisest sulust (`}`).
10. Itereerimine koosneb võtmesõnast `for`, muutuja nimest, võtmesõnast `in`, muutuja nimest, võtmesõnast `do` ja lausest.
11. Itereerimise keha ei tohi omakorda itereerimist sisaldada.
12. Programm koosneb ühest lausest.
13. Tühisümboleid (tühikud, tabulaatorid, reavahetused) tuleb ignoreerida, v.a. literaalides ja operaatorites (näiteks ei ole lubatud `- 5`, vaid peab olema `-5`).


## Lõviosa: JadaCompiler

Klassis _JadaCompiler_ tuleb implementeerida meetod _compile_, mis kompileerib lause CMa programmiks.
Kompileerimisele kehtivad järgmised nõuded:

1. Muutujate väärtused antakse _stack_'il etteantud järjekorras ja pikkustega.
2. Programmi täitmise lõpuks peavad _stack_'il olema ainult muutujate väärtused (etteantud järjekorras ja pikkustega), mis vastavad täitmisjärgsele väärtuskeskkonnale ja on samad nagu _JadaEvaluator_-iga täites.
3. Võib eeldada, et omistamise parema poole avaldises ei loeta omistatavast massiivist.
4. Võib eeldada, et defineerimata muutujaid ei kasutata.

> **PS.** Kuna massiivide pikkused on ette teada ja lühikesed, siis võib omistamise ja itereerimise korral iga elemendi jaoks uuesti omistamise parema poole/itereerimise keha koodi genereerida.
>  Selleks saab kasutada Java tsüklit `for (int i = 0; i < arrayLength; i++)`, mitte CMa-sse kompileeritud tsüklit.
