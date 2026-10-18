"""Read-only r62 art inventory. Writes NEW handoff files; never edits model or Java."""
from pathlib import Path
import json, math, hashlib, bisect, subprocess
import numpy as np

ROOT=Path('D:/银武士协作/repo')
OUT=Path('D:/银武士协作/美术交接')
DOC=ROOT/'docs/art/2026-10-18-r62'
BASE='605d599e4d8dcb11eb70e191e904333e024a8ad4'
PREFIX='2026-10-18_Codex_'
def newfile(path, data):
    path.parent.mkdir(parents=True,exist_ok=True)
    with path.open('x',encoding='utf8',newline='\n') as f: f.write(data)
def deliver(name,data):
    newfile(OUT/(PREFIX+name),data)
    newfile(DOC/name,data)
def vec(x,default=0): return np.array([float(x.get(k,default)) for k in 'xyz'])
def trans(v):
    m=np.eye(4);m[:3,3]=v;return m
def rot(v):
    x,y,z=np.radians(v);cx,sx,cy,sy,cz,sz=math.cos(x),math.sin(x),math.cos(y),math.sin(y),math.cos(z),math.sin(z)
    m=np.eye(4)
    m[:3,:3]=np.array([[cz,-sz,0],[sz,cz,0],[0,0,1]])@np.array([[cy,0,sy],[0,1,0],[-sy,0,cy]])@np.array([[1,0,0],[0,cx,-sx],[0,sx,cx]])
    return m
def serial(v): return [round(float(x),7) for x in v]
class Rig:
    def __init__(self,phase):
        self.phase=phase;self.path=ROOT/'model-source'/f'{phase}.bbmodel'
        self.data=json.loads(self.path.read_text('utf8'));self.groups={g['uuid']:g for g in self.data['groups']}
        self.byname={g['name']:g['uuid'] for g in self.groups.values()};self.parents={};self.cubeparent={}
        def walk(nodes,parent=None):
            for n in nodes:
                if isinstance(n,str):self.cubeparent[n]=parent
                else:self.parents[n['uuid']]=parent;walk(n.get('children',[]),n['uuid'])
        walk(self.data['outliner']);self.anims={a['name']:a for a in self.data['animations']};self.tracks={}
        for a in self.anims.values():
            tracks={}
            for u,t in a['animators'].items():
                for ch in ['position','rotation','scale']:
                    ks=sorted([k for k in t.get('keyframes',[]) if k['channel']==ch],key=lambda k:k['time'])
                    if ks:tracks[(u,ch)]=([k['time'] for k in ks],np.array([vec(k['data_points'][0],1 if ch=='scale' else 0) for k in ks]),[k.get('interpolation')=='step' for k in ks])
            self.tracks[a['name']]=tracks
    def sample(self,name,bone,ch,t):
        track=self.tracks.get(name,{}).get((self.byname.get(bone,bone),ch))
        if not track:return np.ones(3) if ch=='scale' else np.zeros(3)
        times,vals,steps=track
        if t<=times[0]:return vals[0].copy()
        if t>=times[-1]:return vals[-1].copy()
        i=bisect.bisect_right(times,t)-1
        f=0 if steps[i] else (t-times[i])/(times[i+1]-times[i])
        return vals[i]*(1-f)+vals[i+1]*f
    def matrices(self,name,t,normalize_root=True):
        ms={};ss={};root=self.sample(name,'行走_重心','position',t)
        def m(u):
            if u in ms:return ms[u]
            g=self.groups[u];parent=self.parents[u];p=self.sample(name,u,'position',t)
            if normalize_root and parent is None:p=p-root
            s=self.sample(name,u,'scale',t);scale=np.eye(4);scale[:3,:3]=np.diag(s)
            origin=np.array(g['origin']);local=trans(p)@trans(origin)@rot(self.sample(name,u,'rotation',t)+g.get('rotation',[0,0,0]))@scale@trans(-origin)
            ms[u]=(m(parent) if parent else np.eye(4))@local
            ss[u]=(ss[parent] if parent else True) and abs(np.prod(s))>1e-6
            return ms[u]
        for u in self.groups:m(u)
        return ms,ss
    def diff(self,a,ta,b,tb):
        ma,va=self.matrices(a,ta);mb,vb=self.matrices(b,tb);maxd=0;worst='';visibility=[]
        for c in self.data['elements']:
            u=self.cubeparent.get(c['uuid']);parent=u;debris=False
            while parent:
                n=self.groups[parent]['name']
                debris |= n.startswith('脱落_') or self.phase=='phase2' and n=='背挂武士刀_R'
                parent=self.parents[parent]
            if debris:continue
            if u and va[u]!=vb[u]:visibility.append(c['name']);continue
            if u and not va[u]:continue
            origin=np.array(c.get('origin',[0,0,0]));local=trans(origin)@rot(c.get('rotation',[0,0,0]))@trans(-origin)
            ca=(ma[u] if u else np.eye(4))@local;cb=(mb[u] if u else np.eye(4))@local
            for i in range(8):
                p=np.array([c['to'][j] if i&(1<<j) else c['from'][j] for j in range(3)]+[1])
                d=float(np.linalg.norm((ca@p-cb@p)[:3]))
                if d>maxd:maxd=d;worst=c['name']
        return {'max_corner_distance_model_units':round(maxd,6),'scaled_blocks_at_0_1125':round(maxd*.1125,6),'worst_cube':worst,'visibility_changes':visibility}

RIGS=[Rig('phase1'),Rig('phase2')]
reach='攀爬_高位落手抓墙_末步踩墙';first='攀爬_首步连续抬膝_仅起爬使用';loop='攀爬_抬头垂直循环';top='攀爬_架膝踩稳蹬起翻顶'
catch='下坠_空手抢抓_左手先承重';hold='下坠_抓稳检查_待命';release='下坠_无路松手_蹬离墙面';resume='下坠_抓稳后继续攀爬'
stab='下坠_持刀80度猛刺_下滑制动';brace='下坠_插刀稳身_待命';extract='下坠_蹬墙拧腰_借力拔刀脱离'
rstab='下坠_红刀80度猛刺_长距离削墙滑降';rbrace='下坠_红刀滑降稳身_待命';rextract='红刀_下坠_蹬墙拧腰_借力拔刀脱离'
prep='跳跃_压身蹬地';air='跳跃_腾空收腿';land='跳跃_落地缓冲';preview='跳跃_完整审阅'
draw='拔刀_提身上抽_双刀';oldsh='收刀_对准回插_双刀';walk1='行走_背刀负重_循环';mdraw='行进拔刀_双刀低展压迫';msh='行进收刀_低展回插_双刀';full='行进拔收刀_双刀完整流程'
A='战斗_A_后摆拧腰滑步上挑';B='战斗_B_合手蓄力双刀砸地';transition='二阶段_踉跄单手_站直换侧双手立刀';idle2='二阶段_侧身双手立刀_待机';walk2='二阶段_双手立刀_负重行走';combo='二阶段_双手三连_斜劈横斩突进重劈'
def event(name,time=None,end=None,basis='code',source='',note=''):
    return dict(name=name,time_seconds=time,end_seconds=end,basis=basis,source=source,note=note)
release_names=['背挂武士刀_R','脱落_半面罩','脱落_肩甲外板','脱落_胸甲边角','脱落_胫甲缺角','脱落_左腿正面披甲','脱落_右侧裙甲','脱落_前裙外片']
release_times=[.65,.32,.38,.42,.24,.29,.36,.34]
def describe(r,a):
    n=a['name'];ph=r.phase;p=1 if ph=='phase1' else 2;L=a['length'];sh=('双刀' if p==1 else '单刀')+'_收刀同步接近_至松柄';dr=('双刀' if p==1 else '单刀')+'_攀爬登顶再拔刀'
    ev=[event('片段起点',0,basis='asset',source='animations[].length / keyframes'),event('片段终点',L,basis='asset',note='loop 在此回到 0；once/hold 的运行时结束由状态机决定')]
    status='r62 基线已有运行时调用；本次仅补交接，不新增定稿或实机验收结论。'
    if n==draw:use='第一阶段静止拔双刀';start='双刀在背挂，站立';end='双刀已拔出，手落下';before='背刀静止/休眠唤醒';after='armed_idle / armed_walk / A、B（需姿势混合）'
    elif n==oldsh:use='原地回插双刀的备用片段';start='本模型原地拔刀末姿';end='双刀回背挂、手落下';before=draw;after='背刀静止';status='无生产调用；保留的原地收刀备选，不应凭名称替换当前脱战收刀。'
    elif n==walk1:use='第一阶段背刀负重行走；也是 armed_walk 的下身来源';start=end='背刀步态同一循环相位';before='背刀静止或结束收刀';after='自身循环 / 静止 / 行进拔刀（保持步态相位）'
    elif n==mdraw:use='第一阶段边追击边拔双刀';start='背刀行走、身体前压';end='低位外展持双刀';before=walk1;after='armed_walk / armed_idle / 攻击，需混合'
    elif n==full:use='行进拔刀后再收刀的完整展示片段';start='背刀行进';end='回到背刀行进';before='展示起点';after='展示结束或背刀行走';status='无生产调用；审阅/展示用整段，不作为战斗状态机动作。';ev.append(event('拔刀段结束、收刀段开始',2.8,basis='asset-derived',note='2.8s 分段点；不能作为一次攻击'))
    elif n==msh:use='现有第一阶段脱战收刀来源';start='低位外展持双刀';end='背刀行进末姿';before='持刀待机/追击停下';after='背刀静止/休眠；中断时 r62 原路反播到握刀端';ev.append(event('收刀完成',L,source='SamuraiEntity.sheatheTick',note='完整结束才 setDrawn(false)；原地播放下身处理是代码职责'))
    elif n in [A,B,combo]:
        use='前两击斩击，第三击'+('滑步上挑' if n==A else '双刀蓄力砸地' if n==B else '双手单刀突进重劈');start='双刀战斗准备' if p==1 else '双手单刀侧身准备';end='第三击收势后的持刀姿态';before='同阶段持刀待机/追击/恢复';after='恢复混合 → 同阶段持刀待机/追击/下一连招'
        w=([(.424,.625),(1.161,1.394),(2.385,2.56)] if p==2 else [(.30,.47),(1.03,1.24),(2.34,2.51) if n==B else (1.93,2.14)])
        for i,(s,e) in enumerate(w,1):ev.append(event(f'第{i}击刀路判定与突进窗口',s,e,source='CombatTiming.windows / AttackExecution.combo',note='现有代码值，不是模型事件；须真实扫掠接触才伤害；挥刀音效在首次执行此击时'))
        if p==2:ev.append(event('第三击红刀预警',1.985,source='SamuraiEntity.attackTick / CombatFeedback.heavyWarning',note='第三击起点减 0.4s；红刀就绪条件下'))
        ev.append(event('砸地音效/尘土',basis='conditional',source='CombatFeedback.bladeContact',note='第三击有效窗口内刀路实际撞到方块顶面；不是固定秒数，A 上挑不应强行配砸地'))
    elif '侧向快挥' in n:
        use=('第一阶段单手双刀侧切' if p==1 else '第二阶段松辅助手、主手单刀侧切')+'，应对侧面经过的目标';start=end='同阶段持刀准备姿态（结束后接恢复混合）';before='持刀待机/追击';after='持刀待机/追击/连招';s=.19 if p==1 else .24
        ev += [event('侧切有效刀路',s,s+.13,source='SideIntercept.start/end',note=('右侧片段仅 R 刀，左侧仅 L 刀' if p==1 else '两侧片段均为 L 主刀；右手是辅助，不复制第二把刀')),event('停止追加朝向校正',s-.025,source='SideIntercept.canTurn',note='素材秒；不是攻击冷却')]
    elif n==transition:
        use='由完整机甲受击踉跄、掉一把刀，进入战损第二阶段';start='受击前双刀姿态经当前姿势混合';end='站直侧身、双手握 L 单刀柄，刀立在身体另一侧';before='phase1 当前战斗姿态（r62 前 0.22s 姿势混合）';after=idle2+'（握稳停顿和红刃蓄能后）'
        ev += [event('脱落 '+name,t,source='DebrisGeometry.RELEASE',note='本体隐藏和实体生成必须同一权威事件，详见脱落交接') for name,t in zip(release_names,release_times)]
        ev += [event('双手最终握稳',6.0,basis='asset+code',source='RedBladeTiming.gripTicks',note='片段终姿'),event('红刃开始渐亮',6.2,source='RedBladeTiming',note='时间为转阶段状态时间，超出素材6秒；末帧保持0.2s后'),event('红刃蓄能完成',7.1,source='RedBladeTiming',note='18tick/0.9s渐变，非瞬间变红')]
    elif n==idle2:use='第二阶段双手单刀待机';start=end='侧身双手立刀';before='转阶段完成/单刀攻击恢复/登顶再拔刀';after='负重行走/三连/侧切/收刀靠墙'
    elif n==walk2:use='第二阶段双手持刀负重追击';start=end='低重心侧身持刀循环步态';before=idle2;after='待机/三连/侧切/收刀靠墙，步态由实移距离驱动'
    elif n==sh:
        use='持刀追击转攀爬：收刀时同步向墙靠近';start='双刀战斗准备' if p==1 else '侧身双手单刀准备，先处理辅助手';end='刀已回背挂、双手仍在高位，腿已接近墙';before='同阶段持刀待机/追击';after=reach+'，不要先放手到身侧再重新抬手'
        ev.append(event('收刀靠墙片段结束/转落手',L,source='WallTraversal.advance SHEATHE→REACH',note='REACH 入口 wallDrawn(false)；终点不是手已完全放下'))
    elif n==reach:
        use='承接收刀高举手，顺势落手抓墙并末步踩墙';start='背刀、手在高位、末步接近墙';end='双手抓墙、腿承重，准备第一步或翻顶';before=sh+' / 空手靠墙（需入口混合）';after=first+' 或 '+top
        ev.append(event('抓墙准备完成/选首步或翻顶',L,source='WallTraversal.advance REACH',note='小高度可直接TOP；抓点必须世界碰撞验证，不能据终点强行吸附'))
    elif n==first:
        use='起爬首步，左腿顺势连续抬膝，避免小回收';start='抓墙预备';end='完成首步，返回可继续攀爬的支撑相位';before=reach+' / '+hold;after=loop+' 或 '+top
        ev.append(event('首步完成/下一攀爬段',L,source='WallTraversal.advance FIRST',note='仅首次，不要每循环重播；根位移转世界位移一次'))
    elif n==loop:
        use='持续垂直攀爬，抬头看目标、身体适度后仰';start=end='交替攀爬的同一循环相位';before=first+' / 自身循环';after='自身循环 / '+top+' / 支点失效恢复'
        ev.append(event('循环结束/检查顶沿',L,source='WallTraversal.climbNext',note='每周期现有世界上升最多1.35格、末段裁到剩余高度；不是模型根Y累积'))
    elif n==top:
        use='膝盖先架住顶沿、蹬起翻顶、站起双臂顺势落在两侧';start='墙面高位支撑、刀在背挂';end='屋顶站立、刀仍在背挂、双手在身侧略张';before=first+' / '+loop+' / 低墙 '+reach;after=dr
        ev += [event('碰撞箱先上升段结束',1.4112,source='WallTraversal.tick TOP',note='0.42×3.36；只是代码空间路径分段，不是膝盖落点'),event('碰撞箱越沿段结束',3.024,source='WallTraversal.tick TOP',note='(0.42+0.48)×3.36，真实支撑仍须校验'),event('翻顶完成/再拔刀',L,source='WallTraversal.advance TOP',note='站稳后进入DRAW；不能在空中直接设置武器已拔出')]
    elif n==dr:
        use='攀爬登顶后重新拔刀'+('并接回双手单刀立刀' if p==2 else '');start='屋顶站立、背刀、手在身侧';end='双刀握稳' if p==1 else '双手单刀立刀握稳';before=top;after='同阶段持刀待机/追击/攻击，经恢复混合'
        ev.append(event('拔刀及握持转换完成',L,source='WallTraversal.advance DRAW',note='末端 wallDrawn(true)；phase2后段辅手归位未完成前不可当双手已握稳'))
    elif n==catch:
        use='空手下坠时左手先抢支点、右手和腿跟上承重';start='刀在背挂、下坠伸手';end='双手抓稳，身体止坠';before='空手下坠；需可抓握面且空间足够';after=hold
        ev += [event('向抓墙站位对齐的代码结束',.75,source='WallTraversal.tick CATCH',note='不是首手触墙帧；实际抓点接触需验证'),event('抓稳检查入口',L,source='WallTraversal.advance CATCH')]
    elif n==hold:
        use='抢抓后短暂停留，判断能否继续向上';start=end='双手抓稳墙面';before=catch;after=first+'（当前代码）或 '+release
        ev.append(event('检查可继续攀爬',.4,source='WallTraversal.tick HOLD',note='当前运行时锁定素材t=0，等待状态0.4s；不是反复播呼吸循环'))
    elif n==release:
        use='没有向上路线时松手蹬离墙面，接真实下落';start='空手支撑墙面';end='已脱离墙体、向外下落姿势';before=hold+' / 攀爬失败';after='FALL（真实重力）→ 实际着地恢复或再次抢抓'
        ev.append(event('松手动作完成/恢复下落阶段',L,source='WallTraversal.advance RELEASE',note='不能在计时结束时假定已落地'))
    elif n==resume:
        use='抓稳后恢复攀爬的备用周期片段';start=end='抓稳墙面的攀爬循环相位';before=hold;after=loop+' / '+top;status='无生产调用；当前 HOLD→FIRST。素材保留，不必为用满动画增加额外1.6s等待。'
    elif n in [stab,rstab]:
        use=('红刀刺墙后较长距离削墙滑降' if n==rstab else '持刀下坠向墙猛刺并下滑制动');start='手持刀下坠，刀指向墙面法线';end='刀插在墙中、脚踩稳、身体抬头支撑';before='持刀下坠且有实际墙面';after=rbrace if n==rstab else brace
        ev += [event('刺墙对齐代码阶段结束',.5,source='WallTraversal.tick STAB',note='刀须近垂直墙面；模型局部位移不能直接当世界抓点'),event('重新确认支撑',.65,source='WallTraversal.tick STAB',note='t>0.65 且没有抓点时退出；非固定稳定保证')]
        if n==rstab:ev.append(event('红刀接触破块请求',.42,source='WallTraversal.tick STAB',note='一次性 cut；仅 BladeCombat.wallContact 真实接触且配置允许时，不能整面墙自动清空'))
        else:ev.append(event('普通刀刺墙声音/火星',basis='conditional',note='建议仅真实刀尖接触时一次触发，r62模型无事件，不新增伤害判定'))
    elif n in [brace,rbrace]:
        use='刀插墙后踩稳等待，维持刀的墙面方向';start=end='持刀撑墙、双脚承重';before=rstab if n==rbrace else stab;after=rextract if n==rbrace else extract
        ev.append(event('稳身结束→蹬墙拔刀',.4,source='WallTraversal.tick BRACE',note='当前素材采样锁在0，状态等待0.4s；丢失支点走EXTRACT'))
    elif n in [extract,rextract]:
        use='蹬墙借力，腰腹带动手臂把刀拔出，再放松下落';start='刀插墙、脚踩稳';end='刀已离墙，身体后撤下坠，手臂自然放松';before=rbrace if n==rextract else brace;after='真实下落/着地恢复；不能直接瞬移到站立'
        ev += [event('离墙拔刀动作结束',L,source='WallTraversal.advance EXTRACT',note='结束wall状态，重新交给移动系统；拔出瞬间无模型事件，需刀尖/墙面接触边沿确认')]
    elif n==prep:
        use='追击跳跃的压身蓄力蹬地';start='同阶段持刀准备';end='完成蹬地的离地姿态';before='持刀追击且已算出安全落点';after=air
        ev.append(event('实际起跳速度提交',.32,source='PursuitJump.tick PREP',note='再次验证目标、落点与弧线后设置真实速度；不是动画根位移重复推动'))
    elif n==air:
        use='跳跃在空中收腿，维持持刀姿态';start='刚起跳';end='准备落地的腾空末姿';before=prep;after=land+'（由实际onGround触发）'
        ev.append(event('触地→落地动画',basis='conditional',source='PursuitJump.tick AIR',note='真实onGround且airTicks>1；0.9s仅素材长度，飞行更久时持末帧'))
    elif n==land:
        use='真实着地后屈膝缓冲、双臂略展开，再恢复压迫姿态';start='双脚接地后的屈膝入口';end='同阶段持刀准备';before=air+' 的真实落地';after='持刀追击/待机/攻击恢复'
        ev += [event('真实落地/落地声音与尘土',0,basis='conditional',source='PursuitJump.tick AIR→LAND',note='0为进入LAND后的本地时间；音效须接地一次性触发'),event('落地缓冲结束',.43,source='PursuitJump.tick LAND')]
    elif n==preview:
        use='起跳→腾空→落地的固定时长审阅合辑';start='持刀准备';end='落地恢复持刀';before='仅展示';after='仅展示';status='审阅合辑；PursuitJump.clip会识别名称，但真实跳跃状态机只选三个分段，不宜整段硬播。'
        ev += [event('展示起跳分段点',.32,basis='asset-derived',note='不是世界离地保证'),event('展示落地分段点',1.22,basis='asset-derived',note='0.32+0.9；真实游戏不能按此强制落地')]
    else:raise ValueError(n)
    if n in [walk1,walk2,mdraw,msh,full,sh]:
        ev.append(event('左右脚接地音效/尘土',basis='conditional',source='CombatFeedback.footsteps / FootPlantTracker.sample',note='没有固定秒数。phase1离地后净空<0.025格；phase2脚前摆转后支撑且净空<0.09格，均须真实地面。仅素材相位不足以触发。'))
    if n in [reach,first,loop,top,catch,resume,stab,rstab,extract,rextract]:
        ev.append(event('手脚抓握/踩墙/膝盖触沿声',basis='conditional',note='没有已嵌入的固定事件秒数；先用实际手脚位置与墙/顶沿接触验证，不能把root极值当接触帧。'))
    if p==1 and n in (rstab,rbrace,rextract):status='素材保留；共用代码含红刀分支，但 redReady 要求 phase2，第一阶段正常流程不会选择此片段。'
    return dict(purpose=use,start_pose=start,end_pose=end,entry=before,exit=after,status=status,events=ev)

manifest={'schema_version':1,'baseline_commit':BASE,'code_branch_commit':'01901f836ff5eb987996fa42e2512527cee46833','units':'seconds in authored clip, unless event explicitly says state time','not_runtime_config':True,'models':[]}
for r in RIGS:
    records=[]
    for a in r.anims.values():
        rec={'phase':r.phase,'file':f'model-source/{r.phase}.bbmodel','name':a['name'],'uuid':a['uuid'],'length_seconds':a['length'],'loop':a['loop'],**describe(r,a)}
        ks=[k for tr in a['animators'].values() for k in tr.get('keyframes',[])]
        rec['keyframe_count']=len(ks);rec['embedded_events']=[k for k in ks if k['channel'] not in ['rotation','position','scale']]
        rec['effect_track_count']=sum(tr.get('type')=='effect' for tr in a['animators'].values())
        assert not rec['embedded_events']
        assert all(-1e-6<=k['time']<=a['length']+1e-4 for k in ks)
        rec['root_start_model_units']=serial(r.sample(a['name'],'行走_重心','position',0));rec['root_end_model_units']=serial(r.sample(a['name'],'行走_重心','position',a['length']))
        rec['root_delta_model_units']=serial(np.array(rec['root_end_model_units'])-rec['root_start_model_units'])
        rec['gate_intervals']=[]
        for door in ['夹鞘门_L_a','夹鞘门_L_b','夹鞘门_R_a','夹鞘门_R_b']:
            tr=r.tracks[a['name']].get((r.byname[door],'rotation'))
            if not tr:continue
            times,values,_=tr;mask=np.max(np.abs(values),axis=1)>1;segments=[];startidx=None
            for i,on in enumerate(mask):
                if on and startidx is None:startidx=i
                if startidx is not None and (not on or i==len(mask)-1):
                    j=i if on else i-1;segments.append([round(times[startidx],7),round(times[j],7)]);startidx=None
            if segments:rec['gate_intervals'].append({'bone':door,'sampled_open_gt_1_degree':segments,'max_abs_degrees':round(float(np.max(np.abs(values))),5)})
        if a['loop']=='loop':rec['loop_endpoint_comparison']=r.diff(a['name'],0,a['name'],a['length'])
        rec['changes']='本次无素材修改；新增交接文档与只读核验数据；骨骼名、层级、导出格式未变。'
        records.append(rec)
    manifest['models'].append({'phase':r.phase,'sha256':hashlib.sha256(r.path.read_bytes()).hexdigest(),'groups':len(r.groups),'elements':len(r.data['elements']),'animation_count':len(records),'animations':records})

joins=[]
for r in RIGS:
    sh=('双刀' if r.phase=='phase1' else '单刀')+'_收刀同步接近_至松柄';dr=('双刀' if r.phase=='phase1' else '单刀')+'_攀爬登顶再拔刀'
    pairs=[(sh,reach),(reach,first),(reach,top),(first,loop),(loop,top),(top,dr),(catch,hold),(hold,first),(hold,resume),(resume,loop),(hold,release),(stab,brace),(brace,extract),(rstab,rbrace),(rbrace,rextract),(prep,air),(air,land)]
    if r.phase=='phase1':pairs += [(draw,oldsh),(mdraw,msh),(dr,A)]
    else:pairs += [(transition,idle2),(dr,idle2),(idle2,walk2)]
    for a,b in pairs:joins.append({'phase':r.phase,'from':a,'to':b,**r.diff(a,r.anims[a]['length'],b,0)})
manifest['endpoint_joins']=joins
deliver('r62_动画清单与事件来源.json',json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')

intro='''# r62 全动画美术交接

回复对象：鼠王转交的 Claude《银武士共享文件夹、分支与第一批 r62 美术交接请求》（2026-10-18）；代码侧参考 `origin/code/claude-integration:docs/CODE-INTEGRATION.md`。

基线：r62，提交 `605d599e4d8dcb11eb70e191e904333e024a8ad4`。核对代码分支提交 `01901f836ff5eb987996fa42e2512527cee46833`，相对 r62 仅新增代码交接文档，没有另行修改动画调用。phase1 30 个动画、39 组、102 元素；phase2 26 个动画、46 组、185 元素。此交付为说明与数据，不更改任何模型、纹理、音效、骨骼、版本号或代码。

## 时间与验收口径（必须先读）

- **asset**：从 bbmodel 直接读取，时长/循环/轨道事实已确认。
- **asset-derived**：从素材结构推导的分段或几何数值，不是已有事件轨道。
- **code**：r62 代码中的实际判定时刻；仅确认“当前这样执行”，不能冒充美术逐帧确认的接触时刻。
- **conditional**：依赖实际地形、手脚接触或飞行落地，没有一个对所有场景成立的固定秒数。JSON 中为 `null`，绝不解释为 0 秒。
- 所有素材时间以动画内部秒计。连招由 `CombatTiming.animationTime` 非均匀重映射（含最小刀间停顿），不能简单用秒×20得到游戏触发刻；循环行走由位移驱动，也不能直接套墙钟计时。
- 56 个片段实际 **0 个事件关键帧**。`拔刀_提身上抽_双刀` 有 `effects: {name: 效果, type: effect}` 空对象，没有 keyframes；不是一条可用拔刀事件。不要等待它触发。
- `loop` 标签按原值列出。r62采样器只对 loop 做取模，once/hold 均钳在末帧；状态切换仍由代码管理。攀爬 HOLD/BRACE 当前甚至锁采样 t=0，0.4秒是状态等待。
- 门板表中的区间是已有关键帧 **偏转绝对值>1度的采样区间**，便于门声/VFX定位；不是精确开门起止，也不是刀尖出鞘证明。接触/脱离音效仍应以世界几何和状态边沿验证。
- 模型根位移单位不是格：当前比例0.1125格/模型单位。仅允许一个系统提交实体世界位移；不能动画根位移与代码推进各加一次。转阶段、插刀、翻顶和循环攀爬的根位移语义各不相同。
- 本次做静态读取、数值抽样与调用链核对，**没有运行游戏或逐帧人工观看全部片段**。旧版已确认风格保留；真实手掌接触、武器离鞘瞬间及任意地形穿模仍需游戏验收。没有足够证据的时刻不伪造定稿秒数。

## 文件与摘要

JSON `r62_动画清单与事件来源.json` 给出同一清单、UUID、根位移、门板时间和端点误差。`r62_衔接核验.md` 是去除整段根平移后的几何端点比较，用于找必须混合的交接；不是零穿模认证。左右均指角色自身左右。phase2 保留 L 主刀，R 刀是脱落件，不能在后续动作中重新显示。

'''
parts=[intro]
for model in manifest['models']:
    parts += [f"## {model['phase']} · {model['animation_count']} 个动画\n\n源文件：`model-source/{model['phase']}.bbmodel`\n\nSHA-256：`{model['sha256']}`\n\n"]
    for i,a in enumerate(model['animations'],1):
        parts += [f"### {model['phase']}-{i:02d} · {a['name']}\n\n",f"- 阶段/文件：`{a['phase']}` / `{a['file']}`。\n- 时长：**{a['length_seconds']:g} 秒**；循环：`{a['loop']}`；关键帧：{a['keyframe_count']}。\n- 用途：{a['purpose']}。\n- 起始姿势：{a['start_pose']}。接入：{a['entry']}。\n- 结束姿势：{a['end_pose']}。接出：{a['exit']}。\n- 接入状态：{a['status']}\n- 改动：{a['changes']}\n\n| 事件 | 素材秒 / 状态秒 | 依据 | 说明 |\n|---|---|---|---|\n"]
        for e in a['events']:
            t='条件触发，无固定秒数' if e['time_seconds'] is None else f"{e['time_seconds']:g}"+(f"–{e['end_seconds']:g}" if e['end_seconds'] is not None else '')
            parts.append(f"| {e['name']} | {t} | {e['basis']}；{e['source']} | {e['note']} |\n")
        if a['gate_intervals']:
            parts.append('\n门板已有偏转区间（关键帧>1°，不能当出鞘/入鞘判定）：\n\n')
            for g in a['gate_intervals']:parts.append(f"- `{g['bone']}`："+'；'.join(f'{s:.5f}–{e:.5f}s' for s,e in g['sampled_open_gt_1_degree'])+f"；最大偏转 {g['max_abs_degrees']:g}°。\n")
        parts.append(f"\n根位移（模型单位）：首帧 `{a['root_start_model_units']}` → 末帧 `{a['root_end_model_units']}`；差 `{a['root_delta_model_units']}`。不能直接当世界距离。\n\n")
        parts.append('音效/特效补充：本片段没有嵌入事件；未在上表明确列出的声音和粒子没有确认过的固定秒点，不新增常驻循环声音。非攻击片段不自动赋予攻击伤害。\n\n')
deliver('r62_全动画交接.md',''.join(parts).rstrip()+'\n')

report=['# r62 衔接数值核验\n\n回复对象：Claude 第一批 r62 动画交接请求。\n\n']
report.append('方法：对两端关键帧按 r62 的线性/step 通道规则、ZYX旋转、组层级和方块旋转采样；移除行走_重心的整体XYZ平移，排除7块脱落护甲及phase2右刀。比较可见方块8角最大距离。Python双精度独立复现，仅用于静态素材交接；未模拟游戏的StableGrip、入口混合、地形修正、根位移世界转换或碰撞。此处直接采样loop的声明末帧，专门检查首尾接缝，而非运行时取模后的0帧。\n\n**非零不自动等于Bug，尤其攻击、持刀与行走接口通常需要已有恢复混合。零也不证明速度连续或不穿模。** 下表0.001模型单位以下视为数值上近似吻合，不据此自动改变状态机。\n\n|阶段|上段末姿→下段首姿|最大角点差（模型单位）|按0.1125折算格|最大差部件|\n|---|---|---:|---:|---|\n')
for j in joins:report.append(f"|{j['phase']}|{j['from']} → {j['to']}|{j['max_corner_distance_model_units']:.6f}|{j['scaled_blocks_at_0_1125']:.6f}|{j['worst_cube']}|\n")
report.append('\n## 循环首尾\n\n|阶段|动画|最大角点差（模型单位）|\n|---|---|---:|\n')
for m in manifest['models']:
    for a in m['animations']:
        if 'loop_endpoint_comparison' in a:report.append(f"|{m['phase']}|{a['name']}|{a['loop_endpoint_comparison']['max_corner_distance_model_units']:.6f}|\n")
report.append('\n核验范围：56个片段、253946个关键帧均落在声明时长范围内，通道为position/rotation/scale；没有真实事件关键帧。模型哈希见总交接，源文件未写入。准确的手碰墙/刀尖离鞘应在场景几何已知时再测，不能根据这份端点表臆测。\n')
deliver('r62_衔接核验.md',''.join(report))
print(json.dumps({'animations':sum(m['animation_count'] for m in manifest['models']),'keyframes':sum(a['keyframe_count'] for m in manifest['models'] for a in m['animations']),'joins':len(joins),'models':[{k:m[k] for k in ['phase','sha256','animation_count']} for m in manifest['models']]},ensure_ascii=False))
