/**
 * Echter Test von Morni Burgverlies mit Test-Spielern (mineflayer) auf Paper 26.1.2 – gestartet von test/run.sh.
 *   node burgverlies-test.cjs <host> <port> <rcon-host:port> <rcon-passwort-datei> <server-ordner>
 *
 * Spieler: Anna (deutsch, Party-Leiterin mit Bert), Bert (englisch), Carl und Dora (deutsch, ohne Party). Sprache und
 * Party kommen vom Test-Netzwerk (test/fake-network), das auch Statistik, Zustand, „zur Lobby“, „spielt gerade“ und
 * freie Plätze (Verbindung weg) in calls.log mitschreibt. Im Test bleibt ein Platz 12 Sekunden frei.
 *
 * Test-Spieler können nicht richtig kämpfen: Gegner werden per Konsole besiegt (`kill @e[tag=bv_mob]`), dem
 * Kerkermeister wird per `damage` geschadet. Geprüft wird, ob das Spiel darauf richtig reagiert (Wellen, Tore,
 * Abschnitte, Sieg). Den Zustand liest der Test mit `/bvadmin debug <spieler>` (eine Zeile JSON).
 *
 * Verfasst mit Claude 🤖
 */
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const { Vec3 } = require('vec3');
const { rcon } = require('./rcon.cjs');

const [HOST = '127.0.0.1', PORT = '25780', RCON = '127.0.0.1:25880', PASS_FILE, SERVER] = process.argv.slice(2);
const PASS = fs.readFileSync(PASS_FILE, 'utf8').trim();
const VERSION = process.env.MC_VERSION ?? '26.1';
const HALL = { x: 0.5, y: 101, z: 9.5 };
const END_MS = 4000; // ende-sekunden im Test
const RETURN_S = 12; // rueckkehr-sekunden im Test

const results = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
function check(name, ok, detail = '') {
  results.push({ name, ok: Boolean(ok) });
  console.log(`${ok ? '✓' : '✗'} ${name}${detail ? ` – ${detail}` : ''}`);
}

/** Text aus Chat-Komponenten (String, JSON oder NBT) zusammensetzen. */
function plain(v) {
  if (v == null) return '';
  if (typeof v === 'string') {
    if (v.startsWith('{') || v.startsWith('[')) {
      try {
        return plain(JSON.parse(v));
      } catch {
        return v;
      }
    }
    return v;
  }
  if (Array.isArray(v)) return v.map(plain).join('');
  if (typeof v !== 'object') return String(v);
  if (v.type === 'compound') return plain(v.value);
  if (v.type === 'list') return plain(v.value?.value ?? []);
  if (v.type === 'string') return v.value;
  let s = '';
  if ('text' in v) s += plain(v.text);
  if ('' in v) s += plain(v['']);
  if ('translate' in v) s += plain(v.translate);
  if ('extra' in v) s += plain(v.extra);
  return s;
}

function offlineUuid(name) {
  const h = crypto.createHash('md5').update(`OfflinePlayer:${name}`, 'utf8').digest();
  h[6] = (h[6] & 0x0f) | 0x30;
  h[8] = (h[8] & 0x3f) | 0x80;
  const x = h.toString('hex');
  return `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
}

const cmd = (...commands) => rcon(RCON, PASS, commands);
const readFile = (file) => (fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '');
const calls = () =>
  readFile(path.join(SERVER, 'plugins/MoinMorni/calls.log'))
    .split('\n')
    .filter(Boolean)
    .map((l) => JSON.parse(l));
const playerFile = (name) => path.join(SERVER, 'plugins/MorniBurgverlies/spieler', `${offlineUuid(name)}.yml`);

/** Zustand des Abstiegs, in dem der Spieler steckt. */
async function dbg(name) {
  const [out] = await cmd(`bvadmin debug ${name}`);
  try {
    return JSON.parse(out.trim());
  } catch {
    console.log(`  (debug nicht lesbar: ${out.slice(0, 200)})`);
    return { run: null };
  }
}

async function waitFor(cond, ms = 5000, step = 150) {
  const start = Date.now();
  while (Date.now() - start < ms) {
    if (await cond()) return true;
    await sleep(step);
  }
  return Boolean(await cond());
}

/** In der Welt des Spielers teleportieren (ein einfaches tp der Konsole ginge in die Hauptwelt). */
const tp = (name, [x, y, z]) => cmd(`execute at ${name} run tp ${name} ${x} ${y} ${z}`);

let lastJoin = 0;
async function join(name) {
  const wait = lastJoin + 1500 - Date.now();
  if (wait > 0) await sleep(wait);
  lastJoin = Date.now();
  const bot = mineflayer.createBot({ host: HOST, port: Number(PORT), username: name, version: VERSION, auth: 'offline' });
  bot.chats = [];
  bot.bars = [];
  bot.titles = [];
  bot.bossBarSeen = 0;
  bot.on('messagestr', (m, position) => (position === 'game_info' ? bot.bars : bot.chats).push(m));
  bot._client.on('action_bar', (p) => bot.bars.push(plain(p.text)));
  bot._client.on('set_title_text', (p) => bot.titles.push(plain(p.text)));
  bot._client.on('set_title_subtitle', (p) => bot.titles.push(plain(p.text)));
  bot.on('bossBarCreated', () => bot.bossBarSeen++);
  bot.on('kicked', (r) => console.log(`  (${name} gekickt: ${plain(r)})`));
  bot.on('error', (e) => console.log(`  (${name}: ${e.message})`));
  await new Promise((resolve, reject) => {
    bot.once('spawn', resolve);
    bot.once('kicked', (r) => reject(new Error(`${name} gekickt: ${plain(r)}`)));
    setTimeout(() => reject(new Error(`${name}: kein Spawn`)), 20000);
  });
  await sleep(1500);
  return bot;
}

const slot = (bot, i) => bot.inventory.slots[36 + i];
const near = (p, q, d) => Math.abs(p.x - q.x) < d && Math.abs(p.y - q.y) < d && Math.abs(p.z - q.z) < d;
const inHall = (bot) => near(bot.entity.position, HALL, 4) && bot.game.gameMode === 'adventure';
const inDungeon = (bot) => bot.entity.position.x > 900;
const sees = (bot, re) => bot.chats.some((c) => re.test(c));
const sawTitle = (bot, re) => bot.titles.some((t) => re.test(t));
const clear = (...bots) =>
  bots.forEach((b) => {
    b.chats.length = 0;
    b.bars.length = 0;
    b.titles.length = 0;
  });

async function openWithItem(bot, hotbar) {
  bot.setQuickBarSlot(hotbar);
  await sleep(250);
  const opened = new Promise((resolve) => {
    bot.once('windowOpen', resolve);
    setTimeout(() => resolve(null), 4000);
  });
  bot.activateItem();
  return opened;
}

/** Block anklicken: vorher davor stellen (Reichweite), Blick auf den Block. */
async function click(bot, [x, y, z], stand) {
  if (stand) {
    await tp(bot.username, stand);
    await sleep(600);
  }
  const block = bot.blockAt(new Vec3(x, y, z));
  if (!block) return false;
  await bot.lookAt(new Vec3(x + 0.5, y + 0.5, z + 0.5), true);
  await bot.activateBlock(block).catch((e) => console.log(`  (Klick: ${e.message})`));
  return true;
}

/** Ein Block vor einem Wandblock (Richtung Raummitte), auf Fußhöhe. */
function before(block, center, dist = 1.5) {
  const dx = center[0] - block[0];
  const dz = center[2] - block[2];
  if (Math.abs(dx) > Math.abs(dz)) return [block[0] + 0.5 + Math.sign(dx) * dist, center[1], block[2] + 0.5];
  return [block[0] + 0.5, center[1], block[2] + 0.5 + Math.sign(dz) * dist];
}

// ---------------------------------------------------------------- Räume durchspielen

const seen = { types: new Set(), ids: new Set() };
let firstFightDone = false;
let firstTrapDone = false;

/** Ins nächste Raum gehen (Anna geht vor, Bert wird nachgeholt). */
async function enterNext(bots) {
  const d = await dbg('Anna');
  const next = d.next;
  if (!next) return null;
  if (next.type === 'REST') await cmd('damage Bert 6 minecraft:out_of_world');
  await tp('Anna', next.entry);
  const ok = await waitFor(async () => (await dbg('Anna')).room === next.i, 6000);
  if (!ok) return null;
  await sleep(400);
  return (await dbg('Anna')).cur;
}

async function killUntilCleared(index, ms = 40000) {
  return waitFor(async () => {
    const d = await dbg('Anna');
    if (d.room === index && d.cur.phase === 'CLEARED') return true;
    await cmd('kill @e[tag=bv_mob]');
    return false;
  }, ms, 700);
}

async function playFight(bots, room) {
  const [anna, bert] = bots;
  const gate = room.gateIn === 'closed';
  const waited = await waitFor(async () => (await dbg('Anna')).cur.mobs > 0, 6000);
  const d = await dbg('Anna');
  if (!firstFightDone) {
    check(`Kampfraum (${room.id}): Fallgitter hinter einem fällt`, gate, room.gateIn);
    check('Kampfraum: Ausgang bleibt zu, solange gekämpft wird', d.cur.gateOut === 'closed', d.cur.gateOut);
    check('Kampfraum: erste Welle erscheint', waited && d.cur.wave === 0, `Gegner ${d.cur.mobs}, Wellen ${d.cur.waves}`);
    check('Kampfraum: Bert wurde in den Raum geholt', near(bert.entity.position, new Vec3(...room.center), 9),
      JSON.stringify(bert.entity.position));
    check('Wellen-Titel', sawTitle(anna, /Welle 1\/\d/) && sawTitle(bert, /Wave 1\/\d/));
    const [named] = await cmd('data get entity @e[tag=bv_mob,limit=1] CustomName');
    check('Gegner haben eigene Namen', /Kerkerwache|Burgschütze|Kellerspinne|Gruftwächter/.test(named), named.slice(0, 120));
    // Ein Gegner nach dem anderen: nach der ersten Welle kommt die zweite
    await cmd('kill @e[tag=bv_mob]');
    const second = await waitFor(async () => (await dbg('Anna')).cur.wave === 1, 8000);
    check('Nach der ersten Welle kommt die zweite', second);
  }
  const cleared = await killUntilCleared(room.i);
  const after = await dbg('Anna');
  if (!firstFightDone) {
    check('Alle Wellen besiegt → Raum geschafft', cleared, `Welle ${after.cur.wave + 1}/${after.cur.waves}`);
    check('Tore gehen auf', after.cur.gateOut === 'open' && after.cur.gateIn === 'open',
      `${after.cur.gateIn}/${after.cur.gateOut}`);
    check('Belohnung: Kerkertaler für alle', after.heroes.Anna.taler > 0 && after.heroes.Bert.taler > 0,
      `${after.heroes.Anna.taler}/${after.heroes.Bert.taler}`);
    check('Meldung „Raum geschafft“', sees(anna, /Raum geschafft/) && sees(bert, /Room cleared/));
  }
  firstFightDone = true;
  return cleared;
}

async function playTrap(bots, room) {
  const fired = await waitFor(async () => (await dbg('Anna')).cur.fires > 0, 8000);
  let d = await dbg('Anna');
  if (!firstTrapDone || room.crumble > 0) {
    check(`Fallengang (${room.id}): Fallen lösen aus (mit Warnung vorher)`, fired, `Auslöser ${d.cur.fires}`);
  }
  if (d.cur.crumble > 0) {
    // Auf gesprungenen Stein treten: er wackelt kurz und gibt dann nach
    const [tx, ty, tz] = d.cur.tile;
    await tp('Anna', d.cur.tile);
    const gone = await waitFor(async () => /passed/.test((await cmd(
      `execute in minecraft:burgverlies if block ${Math.floor(tx)} ${ty - 1} ${Math.floor(tz)} minecraft:cave_air`))[0]), 3000, 100);
    const fellIn = await waitFor(async () => (await dbg('Anna')).cur.falls > 0, 4000);
    check('Brunnenschacht: gesprungener Stein gibt nach, Anna stürzt und landet am Eingang', gone && fellIn,
      `weg ${gone}, Stürze ${(await dbg('Anna')).cur.falls}`);
    const back = await waitFor(async () => /passed/.test((await cmd(
      `execute in minecraft:burgverlies if block ${Math.floor(tx)} ${ty - 1} ${Math.floor(tz)} minecraft:cracked_stone_bricks`))[0]), 5000, 200);
    check('Brunnenschacht: der Stein liegt nach 3 s wieder da', back);
    // In die Grube unter dem bröckelnden Boden → zurück zum Eingang
    const falls0 = (await dbg('Anna')).cur.falls;
    await tp('Anna', [d.cur.center[0], d.cur.center[1] - 2, d.cur.center[2]]);
    const fell = await waitFor(async () => (await dbg('Anna')).cur.falls > falls0, 4000);
    await sleep(300);
    check('Brunnenschacht: wer in die Grube fällt, landet wieder am Eingang', fell
      && near(bots[0].entity.position, new Vec3(...d.cur.entry), 3), JSON.stringify(bots[0].entity.position));
  }
  // Erst nur eine Person am Ziel → noch nicht geschafft
  await tp('Anna', d.cur.goal);
  await sleep(800);
  d = await dbg('Anna');
  if (!firstTrapDone) {
    check('Fallengang: nur geschafft, wenn alle am Ziel sind', d.cur.phase === 'ACTIVE', d.cur.phase);
  }
  await tp('Bert', d.cur.goal);
  const ok = await waitFor(async () => (await dbg('Anna')).cur.phase === 'CLEARED', 5000);
  if (!firstTrapDone) check('Fallengang: alle am Ziel → geschafft', ok);
  firstTrapDone = true;
  return ok;
}

async function playPuzzle(bots, room) {
  const [anna] = bots;
  await waitFor(async () => (await dbg('Anna')).cur.showing === false, 8000);
  let d = await dbg('Anna');
  const order = d.cur.order;
  const inputs = d.cur.inputs;
  const center = d.cur.center;
  check(`Rätsel (${room.id}): Reihenfolge wird gezeigt (${order.length} Schritte)`, order.length >= 3, JSON.stringify(order));
  const press = async (i) => {
    if (d.cur.plates) {
      await tp('Anna', [inputs[i][0] + 0.5, inputs[i][1], inputs[i][2] + 0.5]);
      await sleep(700);
      await tp('Anna', center);
      await sleep(1100);
    } else {
      await click(anna, inputs[i], before(inputs[i], center));
      await sleep(400);
    }
  };
  // Erst falsch
  const wrong = (order[0] + 1) % inputs.length;
  clear(anna);
  await press(wrong);
  const mistake = await waitFor(async () => (await dbg('Anna')).cur.mistakes === 1, 3000);
  d = await dbg('Anna');
  check('Rätsel: falscher Schritt → von vorn, Spinnen kommen', mistake && d.cur.mobs >= 2 && sees(anna, /Falsch/),
    `Fehler ${d.cur.mistakes}, Gegner ${d.cur.mobs}`);
  await cmd('kill @e[tag=bv_mob]');
  await sleep(500);
  for (const i of order) {
    await press(i);
  }
  const solved = await waitFor(async () => (await dbg('Anna')).cur.phase === 'CLEARED', 4000);
  d = await dbg('Anna');
  check(`Rätsel: richtige Reihenfolge (${d.cur.plates ? 'Platten' : 'Hebel'}) → gelöst`, solved,
    `${d.cur.done}/${order.length}`);
  return solved;
}

async function playTreasure(bots, room) {
  const [anna] = bots;
  const before0 = (await dbg('Anna')).heroes;
  check(`Schatzkammer (${room.id}): sofort offen`, room.phase === 'CLEARED' && room.gateOut === 'open');
  const center = room.center;
  for (const c of room.chests) {
    await click(anna, c, before(c, center));
    await sleep(500);
  }
  const d = await dbg('Anna');
  check('Truhen geöffnet: Taler für alle', d.cur.opened === room.chests.length
    && d.heroes.Bert.taler > before0.Bert.taler && d.heroes.Anna.taler > before0.Anna.taler,
  `${d.cur.opened}/${room.chests.length}, Bert ${before0.Bert.taler}→${d.heroes.Bert.taler}`);
  clear(anna);
  await click(anna, room.chests[0], before(room.chests[0], center));
  await sleep(500);
  check('Leere Truhe gibt nichts mehr', (await dbg('Anna')).heroes.Anna.taler === d.heroes.Anna.taler);
  return true;
}

async function reviveTests(bots) {
  const [anna, bert] = bots;
  // Hinfallen und Wiederbeleben
  clear(anna, bert);
  await cmd('damage Bert 100 minecraft:out_of_world');
  const down = await waitFor(async () => (await dbg('Anna')).heroes.Bert.state === 'DOWN', 3000);
  check('Tödlicher Treffer → Bert liegt am Boden statt zu sterben', down);
  await waitFor(() => sawTitle(bert, /You're down/) && sees(anna, /Bert liegt am Boden/), 2000);
  check('Titel und Hinweis für beide', sawTitle(bert, /You're down/) && sees(anna, /Bert liegt am Boden/),
    `${bert.titles.join(' | ')} / ${anna.chats.slice(-2).join(' | ')}`);
  await tp('Anna', [bert.entity.position.x + 1, bert.entity.position.y, bert.entity.position.z]);
  await sleep(300);
  anna.setControlState('sneak', true);
  const up = await waitFor(async () => (await dbg('Anna')).heroes.Bert.state === 'UP', 6000);
  anna.setControlState('sneak', false);
  const d = await dbg('Anna');
  check('Anna schleicht 3 s neben Bert → wieder auf den Beinen', up, `Wiederbelebungen ${d.heroes.Bert.revives}`);
  check('Kostet eine Wiederbelebung (2 → 1)', d.heroes.Bert.revives === 1 && d.heroes.Bert.downs === 1);
  check('Fortschritt in der Aktionsleiste', anna.bars.some((b) => /Du hilfst Bert/.test(b)), anna.bars.slice(-2).join(' | '));
  // Ausbluten
  await cmd('damage Bert 100 minecraft:out_of_world');
  await waitFor(async () => (await dbg('Anna')).heroes.Bert.state === 'DOWN', 3000);
  const out = await waitFor(async () => (await dbg('Anna')).heroes.Bert.state === 'OUT', 12000);
  await sleep(500);
  check('Niemand hilft → nach 6 s raus (Zuschauer)', out && bert.game.gameMode === 'spectator', bert.game.gameMode);
}

async function playShop(bots, room, floor) {
  const [anna, bert] = bots;
  check(`Krämerstube (Etage ${floor + 1}): Krämerin steht da`, room.npc.length > 0);
  if (floor === 0) await reviveTests(bots);
  await cmd('bvadmin taler Anna 100');
  const before0 = (await dbg('Anna')).heroes.Anna;
  await tp('Anna', room.trader);
  await sleep(800);
  const trader = Object.values(anna.entities).find((e) => e.name === 'villager');
  const opened = new Promise((resolve) => {
    anna.once('windowOpen', resolve);
    setTimeout(() => resolve(null), 4000);
  });
  if (trader) {
    await anna.lookAt(trader.position.offset(0, 1.5, 0), true);
    anna.activateEntity(trader);
  }
  const win = await opened;
  check('Rechtsklick auf die Krämerin öffnet den Laden', win && /Krämerin/.test(plain(win.title)), plain(win?.title));
  if (win && floor === 0) {
    await anna.clickWindow(10, 0, 0);
    await sleep(700);
    const after = (await dbg('Anna')).heroes.Anna;
    check('Schwert gekauft: Stufe 1, 40 Taler weniger, Eisenschwert in der Hand',
      after.sword === 1 && after.taler === before0.taler - 40 && slot(anna, 0)?.name === 'iron_sword',
      `${before0.taler}→${after.taler}, ${slot(anna, 0)?.name}`);
    await anna.clickWindow(22, 0, 0); // Wiederbelebung (80 Taler)
    await sleep(500);
    const rev = (await dbg('Anna')).heroes.Anna;
    if (after.taler >= 80) {
      check('Wiederbelebung gekauft: 80 Taler, 3 statt 2', rev.taler === after.taler - 80 && rev.revives === 3,
        `${after.taler}→${rev.taler}, ${rev.revives}`);
    } else {
      check('Zu wenig Taler → keine Wiederbelebung', rev.taler === after.taler && rev.revives === 2,
        `${rev.taler} Taler, ${rev.revives}`);
    }
  }
  anna.closeWindow(win ?? anna.currentWindow);
  // Alle auf die Treppe
  const fl = (await dbg('Anna')).floor;
  await tp('Anna', room.stairs);
  await sleep(1500);
  const notYet = (await dbg('Anna')).floor === fl;
  if (bert.game.gameMode !== 'spectator') await tp('Bert', room.stairs);
  const down = await waitFor(async () => (await dbg('Anna')).floor === fl + 1, 9000);
  await sleep(800);
  check(`Treppe: alle drauf → Etage ${fl + 2}`, down && (floor > 0 || notYet || bert.game.gameMode === 'spectator'));
  return down;
}

async function playRest(bots, room) {
  const [, bert] = bots;
  await sleep(500);
  const d = await dbg('Anna');
  check(`Rast (${room.id}): Bert geheilt`, bert.health >= 19.5 && d.heroes.Bert.state === 'UP', `${bert.health}`);
  return true;
}

async function playBoss(bots, room) {
  const [anna, bert] = bots;
  // Test-Spieler unverwundbar (Resistenz V): geprüft wird, was der Kerkermeister tut – nicht, wie lange sie leben.
  check('Thronsaal: Fallgitter fällt', room.gateIn === 'closed');
  const spawned = await waitFor(async () => (await dbg('Anna')).cur.boss !== null, 6000);
  let d = await dbg('Anna');
  const boss = d.cur.boss;
  check('Der Kerkermeister erscheint (Leben nach Schwierigkeit und 2 Spielern)', spawned && boss.max === 416,
    JSON.stringify(boss));
  check('Boss-Leiste und Titel', anna.bossBarSeen > 0 && sawTitle(anna, /Der Kerkermeister/) && sawTitle(bert, /The Gaoler/),
    `Leisten ${anna.bossBarSeen}`);
  // Er wartet nicht, bis jemand nah genug ist (früher nur ~12 Blöcke), sondern jagt gleich einen, der steht.
  const hunting = await waitFor(async () => ['Anna', 'Bert'].includes((await dbg('Anna')).cur.boss.target), 3000);
  check('Der Kerkermeister geht gleich auf jemanden los', hunting, JSON.stringify((await dbg('Anna')).cur.boss));
  const shock = await waitFor(async () => (await dbg('Anna')).cur.boss.shockwaves >= 1, 12000);
  check('Abschnitt 1: Schockwelle mit Warnung', shock && anna.bars.some((b) => /Schockwelle/.test(b)));
  // → Abschnitt 2
  d = await dbg('Anna');
  await cmd(`damage @e[tag=bv_boss,limit=1] ${Math.ceil(d.cur.boss.health - d.cur.boss.max * 0.6)} minecraft:generic`);
  const p2 = await waitFor(async () => (await dbg('Anna')).cur.boss.phase === 2, 3000);
  d = await dbg('Anna');
  check('Unter ⅔ Leben → Abschnitt 2 (kurz unverwundbar)', p2 && d.cur.boss.invulnerable > 0, JSON.stringify(d.cur.boss));
  const hp = d.cur.boss.health;
  await cmd('damage @e[tag=bv_boss,limit=1] 50 minecraft:generic');
  await sleep(200);
  check('Während des Wechsels kein Schaden', (await dbg('Anna')).cur.boss.health === hp);
  const summoned = await waitFor(async () => (await dbg('Anna')).cur.boss.summons >= 1, 10000);
  d = await dbg('Anna');
  check('Abschnitt 2: ruft Wachen', summoned && d.cur.mobs > 0, `Wachen ${d.cur.mobs}`);
  check('Titel „Er ruft seine Wachen!“', sawTitle(anna, /ruft seine Wachen/));
  await cmd('kill @e[tag=bv_mob]');
  // → Abschnitt 3
  await sleep(1500);
  d = await dbg('Anna');
  await cmd(`damage @e[tag=bv_boss,limit=1] ${Math.ceil(d.cur.boss.health - d.cur.boss.max * 0.3)} minecraft:generic`);
  const p3 = await waitFor(async () => (await dbg('Anna')).cur.boss.phase === 3, 3000);
  check('Unter ⅓ Leben → Abschnitt 3', p3);
  const chained = await waitFor(async () => (await dbg('Anna')).cur.boss.chains >= 1, 12000);
  check('Abschnitt 3: Ketten (mit grauer Linie vorher)', chained, JSON.stringify((await dbg('Anna')).cur.boss));
  await cmd('kill @e[tag=bv_mob]');
  // Besiegen
  await sleep(2600);
  clear(anna, bert);
  for (let i = 0; i < 10; i++) {
    await cmd('damage @e[tag=bv_boss,limit=1] 400 minecraft:generic');
    await sleep(400);
    const now = await dbg('Anna');
    if (now.stage === 'ENDED' || now.run === null) break;
  }
  const won = await waitFor(() => sawTitle(anna, /Sieg!/) && sawTitle(bert, /Victory!/), 5000);
  check('Kerkermeister besiegt → Sieg!', won, anna.titles.slice(-3).join(' | '));
  check('Zusammenfassung mit Zeit und Punkten', sees(anna, /Geschafft! .*Zeit .*Punkte/));
  return won;
}

async function playRoom(bots, room) {
  seen.types.add(room.type);
  seen.ids.add(room.id);
  switch (room.type) {
    case 'FIGHT':
      return playFight(bots, room);
    case 'TRAP':
      return playTrap(bots, room);
    case 'PUZZLE':
      return playPuzzle(bots, room);
    case 'TREASURE':
      return playTreasure(bots, room);
    case 'REST':
      return playRest(bots, room);
    case 'BOSS':
      return playBoss(bots, room);
    default:
      return true;
  }
}

// ---------------------------------------------------------------- Ablauf

async function main() {
  // ------------------------------------------------------------------ Torhaus
  const anna = await join('Anna');
  const bert = await join('Bert');
  check('Ankunft im Torhaus', inHall(anna) && inHall(bert), JSON.stringify(anna.entity.position));
  check('Schnellleiste: Laterne, Gold, Lohenpulver, Tür',
    slot(anna, 0)?.name === 'lantern' && slot(anna, 4)?.name === 'gold_ingot' && slot(anna, 6)?.name === 'blaze_powder'
      && slot(anna, 8)?.name === 'oak_door', [0, 4, 6, 8].map((i) => slot(anna, i)?.name).join(','));
  check('Deutsch: „Abstieg wählen“', plain(slot(anna, 0)?.customName).includes('Abstieg wählen'));
  check('Englisch (Sprache vom Netzwerk): „Choose your descent“', plain(slot(bert, 0)?.customName).includes('Choose your descent'),
    plain(slot(bert, 0)?.customName));
  check('Begrüßung im Chat', sees(anna, /Willkommen im Torhaus/) && sees(bert, /Welcome to the Gatehouse/));
  check('Leiste rechts', Object.keys(anna.scoreboards ?? {}).length > 0 || anna.scoreboard?.sidebar != null);

  // Bert ist nicht Party-Leiter
  bert.chat('/bv start');
  await waitFor(() => sees(bert, /party leader/), 3000);
  check('Party: nur die Leitung startet (Bert bekommt einen Hinweis)', sees(bert, /party leader/));

  // Anna öffnet das Menü und startet
  const win = await openWithItem(anna, 0);
  check('Laterne öffnet das Abstiegs-Menü', win && /Abstieg/.test(plain(win.title)), plain(win?.title));
  if (win) {
    await anna.clickWindow(13, 0, 0); // Normal
    await sleep(400);
    await anna.clickWindow(20, 0, 0); // Jetzt hinab
  }
  const started = await waitFor(() => inDungeon(anna) && inDungeon(bert), 20000);
  check('„Jetzt hinab“: Anna und Bert (Party) landen zusammen im Verlies', started,
    `${JSON.stringify(anna.entity.position)} / ${JSON.stringify(bert.entity.position)}`);
  check('Titel „Etage 1 · Kellergewölbe“', sawTitle(anna, /Etage 1/) && sawTitle(anna, /Kellergewölbe/) && sawTitle(bert, /Floor 1/));
  check('Startausrüstung: Steinschwert, Bogen, Heiltrank, Pfeile',
    slot(anna, 0)?.name === 'stone_sword' && slot(anna, 1)?.name === 'bow' && slot(anna, 2)?.name === 'potion'
      && slot(anna, 8)?.name === 'arrow', [0, 1, 2, 8].map((i) => slot(anna, i)?.name).join(','));
  await sleep(300);
  check('Netzwerk: Zustand „running“', calls().some((c) => c.t === 'state' && c.state === 'running'));
  check('Netzwerk: Anna und Bert „spielen gerade“', ['Anna', 'Bert'].every((n) =>
    calls().some((c) => c.t === 'playing' && c.name === n && c.playing === true)));
  // Test-Spieler halten Treffer aus (Resistenz V) – Hinfallen prüft der Test mit „out_of_world“-Schaden, der durchgeht.
  await cmd('effect give Anna resistance 900 4 true', 'effect give Bert resistance 900 4 true');
  let d = await dbg('Anna');
  check('Abstieg „normal“, Etage 1, Ankunftsraum offen', d.difficulty === 'normal' && d.floor === 0
    && d.cur.type === 'START' && d.cur.gateOut === 'open', JSON.stringify(d.cur));
  // Geld zum Testen (Owner/Admin über das Verwaltungsrecht): /bvadmin geld
  const talerAnna = d.heroes.Anna.taler;
  const talerBert = d.heroes.Bert.taler;
  const [money] = await cmd('bvadmin geld Anna 50');
  d = await dbg('Anna');
  check('/bvadmin geld Anna 50: +50 Kerkertaler im laufenden Abstieg, mit Bestätigung',
    d.heroes.Anna.taler === talerAnna + 50 && /Anna bekommt 50 Kerkertaler für die Krämerin – jetzt \d+/.test(money.replace(/§./g, '')),
    `${talerAnna} → ${d.heroes.Anna.taler}: ${money.replace(/§./g, '')}`);
  check('Anna liest „Vom Team zum Testen: +50 Kerkertaler“', await waitFor(() => sees(anna, /Vom Team zum Testen: \+50 Kerkertaler/), 3000),
    anna.chats.slice(-2).join(' | '));
  bert.chat('/bvadmin geld Bert 100');
  await sleep(800);
  check('Ohne Verwaltungsrecht (Bert) gibt es kein Geld', (await dbg('Anna')).heroes.Bert.taler === talerBert);

  // ------------------------------------------------------------------ Drei Etagen
  const bots = [anna, bert];
  const plan = [];
  for (let floor = 0; floor < 3; floor++) {
    for (let guard = 0; guard < 12; guard++) {
      d = await dbg('Anna');
      if (d.run === null || d.floor !== floor || !d.next) break;
      const room = await enterNext(bots);
      if (!room) {
        check(`Etage ${floor + 1}: nächsten Raum betreten`, false, JSON.stringify(d.next));
        break;
      }
      plan.push(`${floor + 1}:${room.id}`);
      if (room.type === 'SHOP') {
        await playShop(bots, room, floor);
        break;
      }
      const ok = await playRoom(bots, room);
      if (!ok) {
        console.log(`  (Raum ${room.id} nicht geschafft – Test bricht ab)`);
        floor = 3;
        break;
      }
    }
    if (floor === 0) {
      d = await dbg('Anna');
      check('Neue Etage holt alle zurück (Bert war raus)', d.floor === 1 && d.heroes.Bert.state === 'UP'
        && bert.game.gameMode === 'adventure', `${d.heroes.Bert.state}, ${bert.game.gameMode}`);
      check('Titel „Etage 2 · Kerker“', sawTitle(anna, /Etage 2/));
    }
  }
  console.log(`  Räume: ${plan.join(' → ')}`);
  check('Alle Raumarten kamen vor', ['FIGHT', 'TRAP', 'PUZZLE', 'TREASURE', 'REST', 'BOSS'].every((t) => seen.types.has(t)),
    [...seen.types].join(','));

  // ------------------------------------------------------------------ Ende, Statistik
  const home = await waitFor(() => inHall(anna) && inHall(bert), END_MS + 6000);
  check('Nach dem Sieg zurück ins Torhaus', home, JSON.stringify(anna.entity.position));
  await sleep(1000);
  const stats = calls().filter((c) => c.t === 'stats' && c.name === 'Anna');
  const main0 = stats.find((c) => c.add.runs === 1);
  check('Netzwerk-Statistik: runs 1, wins 1, bosses 1, rooms', main0 && main0.add.wins === 1 && main0.add.bosses === 1
    && main0.add.rooms >= 12 && main0.lower === true, JSON.stringify(main0));
  check('Netzwerk-Statistik: Bestzeit (kleiner ist besser)', main0 && main0.best.best_time_ms > 0, JSON.stringify(main0?.best));
  check('Netzwerk-Statistik: beste Punkte', stats.some((c) => c.best.best_score > 0 && c.lower === false));
  check('Statistik auch für Bert', calls().some((c) => c.t === 'stats' && c.name === 'Bert' && c.add.wins === 1));
  const lastPlaying = (n) => calls().filter((c) => c.t === 'playing' && c.name === n).slice(-1)[0]?.playing;
  check('Netzwerk: nach dem Abstieg „spielt“ niemand mehr', lastPlaying('Anna') === false && lastPlaying('Bert') === false);
  const file = readFile(playerFile('Anna'));
  check('Spielerdatei: 1 Abstieg, 1 Sieg, Bestzeit normal', /abstiege: 1/.test(file) && /siege: 1/.test(file)
    && /bestzeit-normal: [1-9]/.test(file), file.split('\n').slice(0, 6).join(' '));
  check('Bestenliste geschrieben', /Anna, Bert/.test(readFile(path.join(SERVER, 'plugins/MorniBurgverlies/bestenliste.yml'))));
  check('Spur „Glut“ und „Seelenfeuer“ freigeschaltet (nach Sieg über den Kerkermeister)', await (async () => {
    const w = await openWithItem(anna, 6);
    const items = w ? w.slots.slice(0, 27).filter(Boolean).map((s) => s.name) : [];
    if (w) anna.closeWindow(w);
    return items.includes('torch') && items.includes('soul_torch');
  })());

  // ------------------------------------------------------------------ Allein verlieren
  const carl = await join('Carl');
  carl.chat('/bv start leicht');
  const carlIn = await waitFor(() => inDungeon(carl), 15000);
  check('Carl startet allein auf „leicht“', carlIn && (await dbg('Carl')).difficulty === 'leicht');
  check('Leicht: doppelte Pfeile und zwei Heiltränke', slot(carl, 8)?.count === 32 && slot(carl, 2)?.count === 2,
    `${slot(carl, 8)?.count} Pfeile, ${slot(carl, 2)?.count} Tränke`);
  for (let i = 0; i < 2; i++) {
    await cmd('damage Carl 100 minecraft:out_of_world');
    await waitFor(async () => (await dbg('Carl')).heroes.Carl.state === 'DOWN', 3000);
    const self = await waitFor(async () => (await dbg('Carl')).heroes.Carl.state === 'UP', 6000);
    check(`Allein: Carl rappelt sich selbst auf (${i + 1}.)`, self, `noch ${(await dbg('Carl')).heroes.Carl.revives}`);
  }
  clear(carl);
  await cmd('damage Carl 100 minecraft:out_of_world');
  const lost = await waitFor(() => sawTitle(carl, /verschluckt/), 5000);
  check('Keine Wiederbelebung mehr → alle unten → verloren', lost, carl.titles.join(' | '));
  const carlHome = await waitFor(() => inHall(carl), END_MS + 5000);
  check('Carl zurück im Torhaus', carlHome);
  await sleep(500);
  const cs = calls().find((c) => c.t === 'stats' && c.name === 'Carl' && c.add.runs === 1);
  check('Statistik Niederlage: runs 1, wins 0, ohne Bestzeit', cs && cs.add.wins === 0 && !('best_time_ms' in cs.best),
    JSON.stringify(cs));

  // ------------------------------------------------------------------ Mitspieler-Suche
  const dora = await join('Dora');
  clear(carl, dora);
  carl.chat('/bv suchen schwer');
  await sleep(1200);
  check('Suche: Laterne wird zur Barriere „Suche beenden“', slot(carl, 0)?.name === 'barrier');
  dora.chat('/bv suchen schwer');
  const together = await waitFor(() => inDungeon(carl) && inDungeon(dora), 20000);
  const dc = await dbg('Carl');
  const dd = await dbg('Dora');
  check('Suche: Carl und Dora landen nach der Wartezeit im selben Abstieg (schwer)', together && dc.run === dd.run
    && dc.difficulty === 'schwer', `${dc.run}/${dd.run}`);
  carl.chat('/verlassen');
  await sleep(1000);
  check('/verlassen: Carl zurück im Torhaus, Dora bleibt', inHall(carl) && inDungeon(dora));
  dora.chat('/verlassen');
  await waitFor(() => inHall(dora), 5000);
  const [info] = await cmd('bvadmin info');
  check('Alle draußen → Verlies wieder frei', /Abstiege: 0/.test(info.replace(/§./g, '')), info.replace(/§./g, ''));

  // ------------------------------------------------------------------ Zurück ins Spiel (Verbindung weg)
  const mark = calls().length;
  const since = () => calls().slice(mark);
  const holds = (n) => since().filter((c) => c.t === 'hold-slot' && c.name === n).map((c) => c.seconds).join(',');
  clear(carl, dora);
  carl.chat('/bv suchen leicht');
  await sleep(800);
  dora.chat('/bv suchen leicht');
  check('Rückkehr: Carl und Dora zusammen im Abstieg', await waitFor(() => inDungeon(carl) && inDungeon(dora), 25000));
  await cmd('effect give Carl resistance 900 4 true', 'effect give Dora resistance 900 4 true');
  await cmd('damage Dora 5 minecraft:out_of_world');
  await waitFor(() => dora.health <= 15.5, 3000);
  const hpBefore = dora.health;
  const gearBefore = [0, 1, 2, 8].map((i) => slot(dora, i)?.name).join(',');
  let rc = await dbg('Carl');
  const roomBefore = rc.room;
  clear(carl);
  dora.quit();
  check(`Dora weg: Carl erfährt es (Platz ${RETURN_S} s frei, ihr spielt weiter)`, await waitFor(() =>
    sees(carl, new RegExp(`Dora hat die Verbindung verloren – der Platz bleibt ${RETURN_S} Sekunden frei, ihr spielt weiter`)), 4000),
  carl.chats.join(' | '));
  check(`Netzwerk: Platz für Dora frei (holdSlot ${RETURN_S})`, holds('Dora') === String(RETURN_S), holds('Dora'));
  rc = await dbg('Carl');
  check('Dora bleibt im Abstieg (Platz frei), Carl spielt weiter (nicht angehalten)',
    rc.heroes.Dora && !rc.heroes.Dora.left && rc.heroes.Dora.away && !rc.paused, JSON.stringify(rc.heroes.Dora));
  const next = rc.next;
  if (next) await tp('Carl', next.entry);
  check('Die Gruppe geht weiter: Carl betritt ohne Dora den nächsten Raum', next &&
    (await waitFor(async () => (await dbg('Carl')).room === next.i, 6000)), JSON.stringify(next));
  let dora2 = await join('Dora');
  rc = await dbg('Carl');
  check('Dora ist rechtzeitig zurück: bei der Gruppe, im selben Raum wie Carl', inDungeon(dora2) && rc.room !== roomBefore &&
    near(dora2.entity.position, new Vec3(...rc.cur.entry), 3), `${JSON.stringify(dora2.entity.position)} / ${JSON.stringify(rc.cur.entry)}`);
  check('Dora hat ihre Ausrüstung und ihr Leben', [0, 1, 2, 8].map((i) => slot(dora2, i)?.name).join(',') === gearBefore &&
    Math.abs(dora2.health - hpBefore) < 1, `${gearBefore} → ${[0, 1, 2, 8].map((i) => slot(dora2, i)?.name).join(',')}, ${hpBefore} → ${dora2.health}`);
  check('Dora: „Die Gruppe ist schon weiter“', await waitFor(() => sees(dora2, /Die Gruppe ist schon weiter/), 3000),
    dora2.chats.join(' | '));
  check('Carl: „Dora ist wieder da“', await waitFor(() => sees(carl, /Dora ist wieder da/), 3000), carl.chats.join(' | '));
  check('Netzwerk: Platz wieder zu (0), Dora „spielt“ wieder', holds('Dora') === `${RETURN_S},0` &&
    since().filter((c) => c.t === 'playing' && c.name === 'Dora').slice(-1)[0]?.playing === true, holds('Dora'));
  rc = await dbg('Carl');
  check('Dora wieder dabei (nicht weg, steht)', !rc.heroes.Dora.away && rc.heroes.Dora.state === 'UP', JSON.stringify(rc.heroes.Dora));
  // Zu lange weg: wie bisher – Dora verlässt den Abstieg
  clear(carl);
  dora2.quit();
  check('Dora wieder weg: Carl erfährt es', await waitFor(() => sees(carl, /Dora hat die Verbindung verloren/), 4000));
  check(`Zu lange weg (über ${RETURN_S} s): Dora hat das Verlies verlassen`, await waitFor(() =>
    sees(carl, /Dora ist nicht rechtzeitig zurückgekommen und hat das Verlies verlassen/), RETURN_S * 1000 + 6000), carl.chats.join(' | '));
  rc = await dbg('Carl');
  check('Dora ist raus, ihr Abstieg zählt als Niederlage', rc.heroes.Dora.left &&
    since().some((c) => c.t === 'stats' && c.name === 'Dora' && c.add.runs === 1 && c.add.wins === 0), JSON.stringify(rc.heroes.Dora));
  check(`Netzwerk: holdSlot für Dora ${RETURN_S}, 0, ${RETURN_S}, 0`, holds('Dora') === `${RETURN_S},0,${RETURN_S},0`, holds('Dora'));
  dora2 = await join('Dora');
  check('Dora kommt zu spät: im Torhaus mit Hinweis', inHall(dora2) && (await waitFor(() =>
    sees(dora2, /Dein Abstieg ging ohne dich weiter/), 3000)), dora2.chats.join(' | '));
  // Allein unterwegs und weg: der Abstieg hält an, bis Carl zurück ist
  const solo = await dbg('Carl');
  carl.quit();
  await sleep(4000);
  let dp = await dbg('Carl');
  check('Carl allein weg: Abstieg angehalten, die Zeit steht', dp.paused === true && dp.elapsed - solo.elapsed < 1500,
    `paused ${dp.paused}, ${solo.elapsed} → ${dp.elapsed} ms`);
  const carl2 = await join('Carl');
  await sleep(1200);
  dp = await dbg('Carl');
  check('Carl zurück: der Abstieg läuft weiter, im selben Raum', !dp.paused && inDungeon(carl2) && dp.room === solo.room &&
    dp.elapsed - solo.elapsed < 4000, `paused ${dp.paused}, Raum ${solo.room} → ${dp.room}, ${solo.elapsed} → ${dp.elapsed} ms`);
  carl2.chat('/verlassen');
  check('/verlassen nach der Rückkehr: zurück im Torhaus', await waitFor(() => inHall(carl2), 5000));

  // ------------------------------------------------------------------ „Zurück zur Lobby“ (Tür im Torhaus)
  await sleep(500);
  anna.setQuickBarSlot(8);
  await sleep(300);
  anna.activateItem();
  check('Tür „Zurück zur Lobby“ → askToLobby, Anna spielt nicht → gleich in die Lobby',
    await waitFor(() => since().some((c) => c.t === 'ask-to-lobby' && c.name === 'Anna' && c.playing === false), 4000) &&
      !since().some((c) => c.t === 'to-lobby' && c.name === 'Anna'));
  // Älteres Netzwerk-Plugin (ohne Methoden ab Version 2): wie bisher sofort über toLobby
  fs.appendFileSync(path.join(SERVER, 'plugins/MoinMorni/fake.properties'), 'api=1\n');
  bert.setQuickBarSlot(8);
  await sleep(300);
  bert.activateItem();
  check('Älteres Netzwerk-Plugin: Tür schickt wie bisher sofort in die Lobby (toLobby)', await waitFor(() =>
    since().some((c) => c.t === 'old-api' && c.method === 'askToLobby') && since().some((c) => c.t === 'to-lobby' && c.name === 'Bert'), 4000));

  for (const b of [carl2, dora2]) b.quit();
  await sleep(500);
  const failed = results.filter((r) => !r.ok);
  console.log(`\n${results.length - failed.length}/${results.length} Prüfungen bestanden`);
  if (failed.length) {
    console.log('Fehlgeschlagen:');
    failed.forEach((f) => console.log(`  ✗ ${f.name}`));
  }
  process.exit(failed.length ? 1 : 0);
}

main().catch((e) => {
  console.error(e);
  const failed = results.filter((r) => !r.ok);
  console.log(`\n${results.length - failed.length}/${results.length} Prüfungen bestanden (abgebrochen)`);
  process.exit(2);
});
