# GGT Lifesteal

A Fabric mod that adds transferable hearts, combat rules and server progression controls to Minecraft. Players can build their health up to 20 hearts, lose capacity on death, and recover through PvP or crafted Makeshift Hearts.

## Hearts

Players start with **10 hearts**. Permanent capacity ranges from **3 to 20 hearts** and persists across deaths, reconnects and server restarts.

Each death removes one heart of capacity, down to the minimum of three:

- If another player receives kill credit, they receive a Heart item. This includes indirect deaths credited to them, such as knocking someone off a ledge.
- Otherwise, the Heart drops at the victim's death location.
- Players already at three hearts lose no further capacity and generate no Heart reward.
- Kill rewards that cannot fit in the killer's inventory are stored for `/ggtls claim_hearts`.

### Heart item

The regular Heart uses a red custom heart head, a red name and an enchanted glint. Right-clicking consumes it instantly, adds one heart of capacity and fills the new heart. It plays the respawn-anchor charging sound.

At 20 hearts, use is rejected with a message above the hotbar and the item is not consumed. Creative use does not consume the item.

Players can convert spare capacity back into Heart items with `/ggtls withdraw_heart <count>`. Withdrawals must leave **at least 10 hearts**, require inventory space and are blocked during combat.

### Makeshift Heart

A cheaper route back to baseline health, rather than a way to grow beyond it.

Craft one by surrounding a **poisonous potato with eight phantom membranes**:

```text
Membrane  Membrane  Membrane
Membrane  Potato    Membrane
Membrane  Membrane  Membrane
```

Makeshift Hearts have a green name, use a mouldy custom heart head and play a crunchy bone-block sound. Each restores one heart of capacity when the player has **3–9 hearts**, stopping at 10. They are not consumed when the player is already at 10 or more.

Capacity restored with Makeshift Hearts cannot be withdrawn into regular Heart items. Death rewards always generate regular Hearts.

## Combat and logging out

PvP damage places both players in combat for **60 seconds**. Further PvP damage refreshes the timer. Mob damage and fully blocked attacks do not start combat.

While tagged:

- `IN COMBAT: XX` appears above the hotbar.
- Elytras cannot be equipped or used to glide. Existing gliding is stopped.
- Heart withdrawals are blocked.

Logging out during combat drops the player's carried inventory, armour, offhand and temporary crafting contents at their departure location. It also removes one heart of capacity and drops a Heart, unless the player is already at three.

On rejoining, the player is returned to their bed or respawn anchor, with world spawn as the fallback if their personal spawn is unavailable. The pending return survives a server restart.

Normal server shutdowns and kicks issued by this mod's playtime limiter are exempt. Other disconnects during combat are penalised.

## World progression

### Dimension access

The End is locked until an operator runs `/ggtls release_end`. The unlock is saved for that world and survives restarts.

The Nether is independently locked until `/ggtls release_nether`. This applies to existing saves too. Players joining in a locked dimension are returned to Overworld spawn.

### Enchanting

Enchanting starts locked until `/ggtls release_enchants`. Enchanting tables cannot be crafted or used, including crafting through automated crafters. Anvils allow repairs and renaming that preserve enchantments, but reject results that add or upgrade enchantments. Enchanted books can still be collected. The unlock survives restarts.

### Maces and Wardens

The mace recipe requires bedrock, making it unavailable through normal Survival crafting. Instead, Wardens drop **one mace per kill** when mob loot is enabled.

Wardens take **95% less damage from all projectiles**, regardless of anger or range. Shooting from outside sonic-boom range does not bypass this protection.

### Ancient City maps

All vanilla cartographer locator-map trade definitions sell **Ancient City maps** instead of their original destinations. Trade levels, prices, biome eligibility and discounts remain unchanged. Ordinary empty-map trades are unaffected.

Maps use vanilla's nearest-structure search from the cartographer when the offer is generated, including previously discovered cities. The search has a finite range, and maps do not retarget as the player moves. An offer is discarded if no city is found.

Already-generated villager offers and previously purchased maps are unchanged. New offers use the replacement trades.

### Sculk shriekers

Sculk shriekers cannot be mined in Survival and are protected from explosion damage. This applies to natural and player-placed shriekers. Creative players can still break them.

## Locator Compass

A reusable player tracker with a fixed, enchanted Recovery Compass appearance. Right-click to choose an online player from a head menu and confirm spending one **Tracking Charge** for five minutes.

Both recipes use redstone blocks above and below the centre, emerald blocks left and right, and empty corners. Use a compass in the centre for a Locator Compass; use a lapis block for one Tracking Charge, shown as an enchanted echo shard.

The top-centre boss bar shows `Tracking: PLAYER | Distance: XXXm | 04m32s`. Distance is a straight line including height. Across dimensions it shows, for example, `Distance: ? (NETHER)`, while the timer continues.

The target receives “you feel as if you're being watched...” at activation and once per minute. Multiple seekers share that warning interval. Within 100 blocks in the same dimension, both players' combat timers continually refresh to one minute. Normal combat and logout rules apply.

The active compass stays locked in the seeker's inventory. Targets cannot be switched mid-session. Right-click again to confirm cancellation, forfeiting the charge. Seeker death or logout ends the session without refund; target death does not. Target logout refunds one charge, dropping it at the seeker's feet if necessary. A normal server shutdown refunds funded sessions on the next join.

## Dragon's Fury

A reusable, bound potion worth fighting over. Craft it at a crafting table:

```text
Empty          Dragon Egg    Empty
Diamond Block  Glass Bottle  Diamond Block
Empty          Blaze Powder  Empty
```

Drinking grants **Fire Resistance, Speed I and Strength I for five minutes**, including in PvP. Its ten-minute cooldown starts when drinking finishes, persists through restarts and pauses while its owner is offline. It looks like Dragon's Breath when ready and a filled grey potion on cooldown, with the remaining time in its tooltip.

Multiple potions can exist, but each player can own only one, including a pending transfer. Attempting to craft another consumes no ingredients and inflicts Poison I and Blindness for ten seconds. Automated crafters cannot create unbound potions.

The potion can be moved within its owner's inventory but cannot be dropped, stored or handed away. A player-credited kill transfers ownership to the killer and resets its cooldown. If their inventory is full, it waits for the first free slot. If they already own a potion, the victim's potion becomes a dropped bundle containing its egg, bottle, two diamond blocks and blaze powder.

### Spawn vault events

Environmental deaths and combat logouts send the potion to a custom ominous vault at spawn. Its arrival is announced in chat. A floating ten-minute timer and green-to-red bar remain visible through walls within rendering range.

The vault appears above the highest usable surface in a 3×3 chunk area centred on Overworld spawn, including player-built platforms. Dry land is preferred; entirely watery spawn areas permit placement above water. It cannot be broken, exploded, pushed or accessed with keys or hoppers. Surrounding blocks remain editable.

After the timer expires, the first player to right-click with inventory space claims the potion and becomes its owner, with a fresh cooldown. A player already owning one receives its raw materials in a bundle instead. Full inventories receive a warning without consuming the reward.

Events run one at a time in queue order, with a fresh ten-minute timer for each. Queue and timer persist through restarts and pause when nobody is online. If no safe placement exists, the event waits for an operator to clear space and run `/ggtls start_fury_event`.

## Vault Restocks

Surround one ominous trial key with eight trial keys to craft **Vault Restock**, shown as a red crate head. Reverse the keys for **Ominous Vault Restock**, shown as a blue crate head.

Right-click a matching vault to reset your own previous claim instantly. One item is consumed and a matching key is still needed to unlock the vault again. Other players' claims are unchanged. Wrong-type, unclaimed and currently-dispensing vaults reject the item without consuming it.

## Daily playtime limits

Operators can set a daily allowance in whole hours with `/ggtls limit_playtime <hours>`. Setting it to **0** allows unlimited playtime.

- Connected time counts, including AFK time; offline time does not.
- Usage persists across deaths, reconnects and restarts.
- Today's usage is tracked even while the limit is disabled.
- Players receive warnings at five minutes and one minute remaining.
- At zero, they are disconnected and cannot remain connected again until the allowance resets or the limit is changed.
- The limit applies to operators as well as other players.

The allowance resets at **midnight ACST, fixed UTC+09:30**. It does not follow Adelaide daylight saving time. If an operator has exhausted their allowance, the server console can disable the limit.

## Commands

| Command | Access | Description |
| --- | --- | --- |
| `/ggtls` | Everyone | Show command help. |
| `/ggtls status` | Everyone | Show heart capacity, stored rewards, remaining playtime and combat time. |
| `/ggtls withdraw_heart <count>` | Everyone | Convert eligible capacity into Heart items, leaving at least 10 hearts. |
| `/ggtls claim_hearts` | Everyone | Claim stored kill rewards up to available inventory space. |
| `/ggtls set_hearts <player> <count>` | Operator | Set an online player's permanent capacity to 3–20 hearts. Does not heal existing injuries; lowering capacity clamps current health. |
| `/ggtls release_end` | Operator | Unlock the End for the world. |
| `/ggtls release_nether` | Operator | Unlock the Nether for the world. |
| `/ggtls release_enchants` | Operator | Unlock enchanting tables and anvil enchanting. |
| `/ggtls start_fury_event` | Operator | Retry a queued Fury event after clearing a safe spawn location. |
| `/ggtls limit_playtime <hours>` | Operator | Set the daily allowance; 0 disables the limit. |

Examples:

```mcfunction
/ggtls withdraw_heart 2
/ggtls claim_hearts
/ggtls set_hearts @s 15
/ggtls limit_playtime 3
/ggtls release_end
```

## Installation and building

This project targets **Minecraft 26.3**, **Java 25**, **Fabric Loader 0.19.5** and **Fabric API 0.161.0+26.3**.

Install the built mod JAR and the matching Fabric API in the Fabric installation's `mods` folder. Install the mod on the server and participating clients so the custom items are available on both sides. Use the normal mod JAR, not the `-sources.jar`.

To build on Windows, run this from the project directory:

```powershell
.\gradlew.bat build
```

The output is written to `build/libs/`. Stop the server before replacing its existing copy of the mod, then restart it. Avoid leaving multiple versions of the mod in the folder.

For IntelliJ development, stop and relaunch the Minecraft Client run configuration after Java changes.

## Testing status

The mod is in development. Source compilation and targeted checks have been performed, but the complete multiplayer feature set has not been independently verified end to end. Test updates in a separate world before using them on an established server.
