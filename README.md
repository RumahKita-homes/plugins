# RumahKita Plugins Repository

Welcome to the specific Source Code Repository for **RumahKita Server**. This repository contains a collection of legacy plugins (extracted/decompiled) as well as new custom-developed plugins for internal server needs.

## Repository Structure 

All plugin code is separated by category. You can open each plugin's folder to view the source code (`src/main/java`) and read the `README.md` file inside to understand the specific function of that plugin.

*   `src-plugins/phy0n/` - Collection of core server plugins (Admin, EconomyV2, Warps, Trade, etc).
*   `src-plugins/RumahKitaGuilds_src/` - Guild & Faction System.
*   `src-plugins/RumahKitaAntiXray_src/` - Anti X-Ray Security System.
*   `src-plugins/RumahKitaDiscordVerify_src/` - Discord Bot Synchronization.
*   *And many other plugins...*

## How to Build 

All plugins in this repository have been converted to use the **Maven** structure format.

To compile (build) them into `.jar` files ready for use on the server:
1. Open Terminal/Command Prompt.
2. Enter the plugin folder you want to build. (Example: `cd src-plugins/phy0n/RumahKitaEconomyV2_src`).
3. Run the Maven command: `mvn clean package`.
4. Retrieve the compiled output from the `/target` folder.

## Policy & Security 

*   The code in this repository is **Private** and designed specifically for RumahKita infrastructure.
*   Security has been thoroughly *audited* (free from *Item Dupe*, *Money Dupe* exploits, and Admin/OP access vulnerabilities).

---
*Created and maintained to provide the best playing experience on RumahKita Server.*
