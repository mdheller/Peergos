$wnd.showcase.runAsyncCallback11("function rjc(){}\nfunction tjc(){}\nfunction mjc(a,b){a.b=b}\nfunction ojc(a){if(a==bjc){return true}BB();return a==ajc}\nfunction njc(a){if(a==cjc){return true}BB();return a==fjc}\nfunction sjc(a){this.b=(Vkc(),Qkc).a;this.e=($kc(),Zkc).a;this.a=a}\nfunction pjc(){gjc();jec.call(this);this.b=(Vkc(),Qkc);this.c=($kc(),Zkc);this.e[MRc]=0;this.e[NRc]=0}\nfunction kjc(a,b){var c;c=rfb(a.fb,180);c.b=b.a;!!c.d&&(c.d[LRc]=b.a,undefined)}\nfunction ljc(a,b){var c;c=rfb(a.fb,180);c.e=b.a;!!c.d&&iec(c.d,b)}\nfunction gjc(){gjc=FCb;_ic=new rjc;cjc=new rjc;bjc=new rjc;ajc=new rjc;djc=new rjc;ejc=new rjc;fjc=new rjc}\nfunction hjc(a,b,c){var d;if(c==_ic){if(b==a.a){return}else if(a.a){throw _Bb(new UBc('Only one CENTER widget may be added'))}}Oh(b);fvc(a.j,b);c==_ic&&(a.a=b);d=new sjc(c);b.fb=d;kjc(b,a.b);ljc(b,a.c);jjc(a);Qh(b,a)}\nfunction ijc(a){var b,c,d,e,f,g,h;Ouc(a.hb,'',ETc);g=new JJc;h=new pvc(a.j);while(h.b<h.c.c){b=nvc(h);f=rfb(b.fb,180).a;d=rfb(REc(_Jc(g.d,f)),112);c=!d?1:d.a;e=f==djc?'north'+c:f==ejc?'south'+c:f==fjc?'west'+c:f==ajc?'east'+c:f==cjc?'linestart'+c:f==bjc?'lineend'+c:tQc;Ouc(Qo(b.hb),ETc,e);bFc(g,f,iCc(c+1))}}\nfunction jjc(a){var b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r;b=a.d;while(b.children.length>0){to(b,b.children[0])}o=1;e=1;for(i=new pvc(a.j);i.b<i.c.c;){d=nvc(i);f=rfb(d.fb,180).a;f==djc||f==ejc?++o:(f==ajc||f==fjc||f==cjc||f==bjc)&&++e}p=Aeb(ywb,oNc,289,o,0,1);for(g=0;g<o;++g){p[g]=new tjc;p[g].b=Uo($doc,JRc);po(b,qbc(p[g].b))}k=0;l=e-1;m=0;q=o-1;c=null;for(h=new pvc(a.j);h.b<h.c.c;){d=nvc(h);j=rfb(d.fb,180);r=Uo($doc,KRc);j.d=r;j.d[LRc]=j.b;j.d.style[ARc]=j.e;j.d[FNc]=j.f;j.d[ENc]=j.c;if(j.a==djc){mbc(p[m].b,r,p[m].a);po(r,qbc(d.hb));r[USc]=l-k+1;++m}else if(j.a==ejc){mbc(p[q].b,r,p[q].a);po(r,qbc(d.hb));r[USc]=l-k+1;--q}else if(j.a==_ic){c=r}else if(njc(j.a)){n=p[m];mbc(n.b,r,n.a++);po(r,qbc(d.hb));r[FTc]=q-m+1;++k}else if(ojc(j.a)){n=p[m];mbc(n.b,r,n.a);po(r,qbc(d.hb));r[FTc]=q-m+1;--l}}if(a.a){n=p[m];mbc(n.b,c,n.a);po(c,qbc(a.a.hb))}}\nvar ETc='cwDockPanel';ECb(445,1,sQc);_.Bc=function wVb(){var a,b,c;YEb(this.a,(a=new pjc,a.hb.className='cw-DockPanel',a.e[MRc]=4,mjc(a,(Vkc(),Pkc)),hjc(a,new Ohc('This is the first north component'),(gjc(),djc)),hjc(a,new Ohc('This is the first south component'),ejc),hjc(a,new Ohc('This is the east component'),ajc),hjc(a,new Ohc('This is the west component'),fjc),hjc(a,new Ohc('This is the second north component'),djc),hjc(a,new Ohc('This is the second south component'),ejc),b=new Ohc(\"This is a <code>ScrollPanel<\\/code> contained at the center of a <code>DockPanel<\\/code>.  By putting some fairly large contents in the middle and setting its size explicitly, it becomes a scrollable area within the page, but without requiring the use of an IFRAME.<br><br>Here's quite a bit more meaningless text that will serve primarily to make this thing scroll off the bottom of its visible area.  Otherwise, you might have to make it really, really small in order to see the nifty scroll bars!\"),c=new ifc(b),c.hb.style[FNc]='400px',c.hb.style[ENc]='100px',hjc(a,c,_ic),ijc(a),a))};ECb(901,281,MNc,pjc);_.gc=function qjc(a){var b;b=gdc(this,a);if(b){a==this.a&&(this.a=null);jjc(this)}return b};var _ic,ajc,bjc,cjc,djc,ejc,fjc;var zwb=DBc(JNc,'DockPanel',901);ECb(179,1,{},rjc);var wwb=DBc(JNc,'DockPanel/DockLayoutConstant',179);ECb(180,1,{180:1},sjc);_.c='';_.f='';var xwb=DBc(JNc,'DockPanel/LayoutData',180);ECb(289,1,{289:1},tjc);_.a=0;var ywb=DBc(JNc,'DockPanel/TmpRow',289);VMc(wl)(11);\n//# sourceURL=showcase-11.js\n")