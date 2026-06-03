# Vaevaline vektorkeel Vex

Vex on aritmeetiliste avaldiste keel, kus kasutatakse kahte tüüpi väärtusi ja avaldisi: täisarvulised skalaarid ja kahemõõtmelised vektorid.
Vektoritega saab teha matemaatikast tuntud operatsioone: liitmine, skalaariga korrutamine, projektsioon ja skalaarkorrutis.

Järgmine näide on Vexi avaldis, mis liidab kahte vektorit:
```
(X.X)*YR + <X|y/2, Z|x+10>
```
Esimese vektori saamiseks pööratakse vektor `Y` paremale ja korrutatakse skalaariga `X.X` (vektori `X` skalaarkorrutis tema endaga). Teine vektor moodustatakse koordinaathaaval: x-koordinaadiks on vektori `X` y-koordinaat jagatud kahega ja y-koordinaadiks on vektori `Z` x-koordinaat pluss kümme.

Keele AST klassid paiknevad _eksam1.ast_ paketis ja nende ülemklassiks on _VexNode_:

* _VexScalarNode_ — skalaaravaldised, alamklassidega:
    * _VexNum_ — arvliteraal;
    * _VexSVar_ — skalaarmuutuja;
    * _VexBinOp_ — skalaaride binaarsed operaatorid (_Add_, _Sub_, _Mul_, _Div_);
    * _VexProj_ — vektori projektsioon (_X_- või _Y_-teljele);
    * _VexDot_ — kahe vektori skalaarkorrutis;
* _VexVectorNode_ — vektoravaldised, alamklassidega:
    * _VexVec_ — vektorliteraal;
    * _VexVVar_ — vektormuutuja;
    * _VexScale_ — vektori korrutamine skalaariga;
    * _VexPlus_ — kahe vektori liitmine.

Klassis _VexNode_ on staatilised abimeetodid, millega saab mugavamalt abstraktseid süntaksipuid luua.
Ülalolev avaldis moodustatakse järgmiselt:

```
plus(
    scale(
        dot(vvar("X"), vvar("X")),
        vec(proj(Y, vvar("Y")), mul(num(-1), proj(X, vvar("Y"))))),
    vec(
        div(proj(Y, vvar("X")), num(2)),
        add(proj(X, vvar("Z")), num(10))))
```

> **NB!** Kuna pööramise operatsioon ei esine AST klassina, siis on see esitatud põhiosas antud valemi põhjal.


## Alusosa: VexEvaluator

Klassis _VexEvaluator_ tuleb implementeerida meetod _eval_, mis väärtustab vektoravaldise etteantud väärtuskeskkonnas.
Väärtustamisele kehtivad järgmised nõuded:

1. Arvliteraalid, muutujad ja skalaaride binaarsed operatsioonid käituvad standardselt.
2. Vektorliteraali väärtuseks on vektor, mille koordinaadid on vastavalt antud skalaarid.
3. Skalaarmuutujate ja vektormuutujate keskkonnad on ette antud eraldi.
4. Projektsiooni väärtuseks on skalaar, mis on antud vektori vastav koordinaat.
5. Vektori korrutamisel skalaariga korrutatakse antud skalaariga mõlemad vektori koordinaadid.
6. Vektorite liitmisel liidetakse vastavad koordinaadid.
7. Vektorite `<x,y>` ja `<z,w>` skalaarkorrutis on skalaar väärtusega `x*z + y*w`.
8. Võib eeldada, et defineerimata muutujaid ei kasutata.

> **PS.** Klassides _VexBinOp_ ja _Vector_ on vajalikud operaatorid juba defineeritud.


## Põhiosa: VexAst

Failis _Vex.g4_ tuleb implementeerida grammatika ja klassis _VexAst_ tuleb implementeerida meetod _parseTreeToAst_, mis teisendab parsepuu AST-iks.
Süntaksile kehtivad järgmised nõuded:

1. _Lihtne_ skalaaravaldis võib olla üks järgmistest:
    1. Arvliteraal, mis koosneb numbritest, millele võib eelneda miinusmärk. Esimene number tohib olla 0 ainult siis, kui see on arvu ainuke number.
    2. Skalaarmuutuja, mis koosneb vähemalt ühest ladina väiketähest.
    3. Sulgudes keeruline skalaaravaldis (vt allpool).
2. _Keeruline_ skalaaravaldis võib lisaks lihtsale skalaaravaldisele olla üks järgmistest:
    1. Binaarne aritmeetiline operaator kahe keerulise skalaaravaldisest argumendiga: liitmine (`+`), lahutamine (`-`), korrutamine (`*`) või jagamine (`/`). Nende puhul kehtivad standardsed assotsiatiivsused ja prioriteedid.
    2. Projektsioon, mis koosneb lihtsast vektoravaldisest (vt allpool), püstkriipsust (`|`) ja koordinaattelje tähisest (`x` või `y`).
    3. Skalaarkorrutis, mis koosneb lihtsast vektoravaldisest (vt allpool), punktist (`.`) ja lihtsast vektoravaldisest (vt allpool).
3. _Lihtne_ vektoravaldis võib olla üks järgmistest:
    1. Vektorliteraal, mis koosneb avavast nurksulust (`<`), keerulisest skalaaravaldisest, komast (`,`), keerulisest skalaaravaldisest ja lõpetavast nurksulust (`>`).
    2. Vektormuutuja, mis koosneb ühest ladina suurtähest ning nullist või enamast ladina väiketähest.
    3. Pööramine, mis koosneb lihtsast vektoravaldisest ja suuna tähisest (`L` või `R`). Need tuleb AST-is esitada olemasolevate konstruktsioonide kaudu, kasutades samaväärsusi: `ZL` ≡ `<-1*Z|y, Z|x>` ja `ZR` ≡ `<Z|y, -1*Z|x>`.
    4. Sulgudes keeruline vektoravaldis (vt allpool).
4. _Keeruline_ vektoravaldis võib lisaks lihtsale vektoravaldisele olla üks järgmistest:
    1. Vektori korrutamine skalaariga, mis koosneb lihtsast skalaaravaldisest, korrutamisest (`*`) ja lihtsast vektoravaldisest.
    2. Liitmine, mis koosneb keerulisest vektoravaldisest, liitmisest (`+`) ja keerulisest vektoravaldisest.
5. Programm koosneb ühest keerulisest vektoravaldisest.
6. Tühisümboleid (tühikud, tabulaatorid, reavahetused) tuleb ignoreerida, v.a. literaalides ja operaatorites (näiteks ei ole lubatud `- 5`, vaid peab olema `-5`).

> **PS.** Kuna skalaaravaldised ja vektoravaldised on erinevat tüüpi ning võivad teineteist vastastikuselt sisaldada, siis on visitori kirjutamine veidi keerulisem. Selleks on kaks võimalust:
>
> 1. Implementeerida isendiväljades eraldi visitorid skalaaride ja vektorite teisendamiseks, sest nii saavad nad teineteise meetodeid välja kutsuda. Klassis _VexAst_ on selleks mall ette antud.
> 2. Implementeerida üks üldine visitor, mis tagastab _VexNode_ ja vajadusel _cast_'ib õigesti.


## Lõviosa: VexCompiler

Klassis _VexCompiler_ tuleb implementeerida meetod _compile_, mis kompileerib vektoravaldise CMa programmiks.
Kompileerimisele kehtivad järgmised nõuded:

1. Muutujate väärtused antakse _stack_'il etteantud indeksitel. Vektormuutuja korral on etteantud indeksil selle x-koordinaadi väärtus ja y-koordinaadi väärtus on ühe võrra suuremal indeksil.
2. Programmi täitmise lõpuks peab _stack_'i pealmine element olema vektoravaldise y-koordinaadi väärtus ja vahetult selle all olev element peab olema vektoravaldise x-koordinaadi väärtus. Saadud vektor peab olema sama nagu _VexEvaluator_-iga väärtustades.
3. Programmi täitmise lõpuks tohivad _stack_'il olla ainult etteantud muutujate algsed väärtused ja arvutatud avaldise väärtus.
4. Võib eeldada, et defineerimata muutujaid ei kasutata.

> **PS.** Vektoravaldised on mõistlik väärtustada koordinaathaaval, st genereerida eraldi kood x- ja y-koordinaadi arvutamiseks. Seejuures on vaja kompileerimise ajal meeles pidada, kumma koordinaadi jaoks parasjagu koodi genereeritakse. Klassis _VexCompiler_ on selleks mall ette antud.
