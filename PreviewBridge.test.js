// Test the browser/native contract without network or third-party packages.
const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const html = fs.readFileSync(__dirname + '/preview/index.html', 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];
const messages = [], calls = [], listeners = {}, intervals = [];
let options;
const api = {
  setVolume: n => calls.push(['volume', n]), playVideo: () => calls.push(['play']),
  pauseVideo: () => calls.push(['pause']), seekTo: n => calls.push(['seek', n]), unMute: () => calls.push(['unmute']),
  getPlayerState: () => 1, getCurrentTime: () => 12.5, getDuration: () => 120,
  isMuted: () => false, getVolume: () => 35
};
const window = {chrome: {webview: {postMessage: x => messages.push(x), addEventListener: (k,f) => listeners[k] = f}}, addEventListener: (k,f) => listeners[k] = f};
const context = {window, URLSearchParams, navigator:{onLine:true}, location:{search:'?id=M7lc1UVf-VE&token=test&volume=35', origin:'https://pocketmusic.desktop'}, document:{createElement:()=>({}),head:{appendChild:()=>{}}}, setTimeout:()=>{}, setInterval:f=>intervals.push(f), YT:{Player:function(_, config){options=config;return api;}}};
vm.runInNewContext(script,context);
window.onYouTubeIframeAPIReady();
assert.equal(options.videoId,'M7lc1UVf-VE');
assert.equal(options.playerVars.origin,'https://pocketmusic.desktop');
assert.equal(options.playerVars.controls,1);
options.events.onReady();
assert.ok(messages.some(x=>x.type==='ready'));
assert.ok(messages.some(x=>x.type==='status' && x.position===12.5 && x.duration===120 && x.volume===35));
for (const action of ['play','pause','seek','volume']) listeners.message({data:{token:'test',action,value:45}});
assert.ok(calls.some(x=>x[0]==='pause'));
assert.ok(calls.some(x=>x[0]==='seek'&&x[1]===45));
assert.ok(calls.some(x=>x[0]==='volume'&&x[1]===45));
const count=calls.length;
listeners.message({data:{token:'stale',action:'play'}});
listeners.message({data:{token:'test',action:'seek',value:'untrusted text'}});
listeners.message({data:{token:'test',action:'execute',value:'untrusted text'}});
assert.equal(calls.length,count);
options.events.onAutoplayBlocked();
assert.equal(messages.at(-1).type,'blocked');
const beforeTicks = messages.length;
context.document.hidden = true;
for (let i=0;i<4;i++) intervals[0]();
assert.equal(messages.length, beforeTicks, 'Hidden preview should not flood the native UI');
intervals[0]();
assert.equal(messages.length, beforeTicks+1, 'Hidden preview must retain its heartbeat');
context.document.hidden = false;
intervals[0]();
assert.equal(messages.length, beforeTicks+2, 'Visible playback should refresh its position');
listeners.offline();
assert.equal(messages.at(-1).code,'offline');
for(const code of [2,5,100,101,150,153]) { options.events.onError({data:code});assert.equal(messages.at(-1).code,String(code)); }
console.log('PASS: iframe bridge controls, progress, token isolation, autoplay-blocked, offline and six YouTube errors.');
