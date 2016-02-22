### 1.30.3:
* Fix missing op labels/summaries
* Fix issue in "Show changed"
* Fix OP_BOOT_COMPLETED on Marshmallow

### 1.30.2
* Fixed crashes

### 1.30.1
* Fixed crash
* Updated Slovak translation

### 1.30
* Better support for Marshmallow
* Fix crashes
* Fix label/summary issues
* Fix multiple icons in "App info" screen

### 1.29
* Add option to customize appearance (theme, icons)
* Fixed issue on MiUi where ops were reset
* Add AppOps to AOSP Lollipop Settings
* Add option to export/import app restrictions
* AOX now displays notifications on app installs/updates,
  launching the AppOps screen for that particular app

### 1.28.1
* Fix bugs in hacks for OP_WAKE_LOCK and OP_BOOT_COMPLETED

### 1.28
* Fix crashes on certain HTC devices
* Fix switch labels
* Smaller icon in Settings

### 1.27
* Add translations from CM-12 for a ton of languages

### 1.26.1
* Restrictions can be reset under "Show changed"

### 1.26
* Fix crash when changing ops
* Better handling of non-AOSP ops (labels, summaries)
* Fix issue on Sony ROMS where all ops are off by default

### 1.25.3
* Fix several Lollipop issues

### 1.24
* Add option to disable verbose logs
* Potential fix for LG icon issue
* Potential fix for Samsung GridSettings bug
* Add hack to fix ops resetting on reboot if
  installed on SD.

### 1.23
* Finally got the HTC variant working
  (thanks to Mikanoshi@XDA)
* Fixed some system-app-install issues
* Bugfixes

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
