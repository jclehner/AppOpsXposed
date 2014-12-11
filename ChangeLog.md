### 1.22
* AppOpsXposed now works without Xposed, by installing
  as a system-app.
* Add new ops for Lollipop (compatibility mode)
* When enabled, use compatibility mode when launching
  from settings app as well

### 1.21.1
* Fix compatibility mode

### 1.21
* Added new compatibility mode (BETA)
* Fix crash on LG ROMs
* Attempted to fix wrong icon size on some ROMs
* WakeLockFix disabled on JellyBean for now

### 1.20.2
* Fix version number (updates should work now)

### 1.20.1
* Fix settings icon

### 1.20
* Fixed compatibility with some LG ROMs
* Fixed WakeLockFix (JellyBean currently broken)
* Updated icon in settings for non-AOSP ROMs

### 1.19
* Fixed some issues in Samsung ROMs
* Fixed issue in detection of CyanogenMod-based ROMs
* Added bug report functionality

### 1.18
* Added OP_BOOT_COMPLETED hack (MUST BE ENABLED MANUALLY)
* Added WakeLock-fix hack (MUST BE ENABLED MANUALLY)
* Fixed crashes in CyanogenMod-based ROMs
* Added Spanish translation (Jose Artuñedo@XDA)

### 1.17.2
* Xperia only: more human readable info
  (e.g. "Run at start-up" vs "BOOT_COMPLETED")

### 1.17:
* Fixed compatibility with Galaxy S5 settings app
  (grid layout)
* Fixed crash on Xperia KitKat ROMs

### 1.16:
* Added OmniROM variant (no header in settings, only
  icon in "App info")
* Added variant for Sony KitKat ROMs with a switch
  in AppOps, as opposed to the drop-down menu found
  in 4.3 ROMs.

### 1.15.1:
* Fix crashes on Android 4.3

### 1.15:
* Added module-specific settings (click module name in 
  Xposed Installer's "Module" section)
* Launcher icon can now be hidden
* Added Korean translation by @SDKoongchi
