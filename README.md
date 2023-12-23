## spigot-2fa

Zero configuration time-based one time password authentication plugin for SpigotMC.

### Why?

Sometimes you enter [Minecraft phase](https://www.urbandictionary.com/define.php?term=minecraft%20phase)
and want to have fun with your boys for a couple of weeks. Not all the boys own a licensed
copy of Minecraft which effectively leaves your server completely unprotected.

Enter this plugin. All the boys are now authenticated with TOTP (Google Authenticator, etc.),
and no griefer can sneak in. No whitelist setup is required!

### Features

* Zero configuration: drop the JAR file in `plugins` directory, and you're good to go.
* Provides TOTP authentication using chat commands (RFC 6238).
* Unauthenticated players are put in a *perfect jail*: they can only leave or authenticate.
* Automatic authentication for LAN IPs (192.168.0.0/16, etc.).
* Last player IP caching for automatic authentication.

### Commands

**Command**: `/2fa <player_name>`

**Description**: Allow or reset authentication setup for a given player. 
The command must be executed either from console or by an authenticated user. 
Target player must be online.

---

**Command**: `/code <totp_code>`

**Description**: Use this command in order to authenticate with your TOTP code.

### Installation

Drop the JAR file in the server `plugins` directory.
