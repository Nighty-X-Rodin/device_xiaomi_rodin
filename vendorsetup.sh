
 
# Auto-apply source side patches for rodin

#!/bin/bash
# Camera and device patches for Xiaomi Rodin

# Clone BypassCharge app if not present
if [ ! -d "packages/apps/BypassCharge" ]; then
    echo "[Rodin] Cloning BypassCharge to packages/apps/BypassCharge..."
    git clone https://github.com/TheXPerienceProject/android_packages_apps_BypassCharge.git packages/apps/BypassCharge
else
    echo "[Rodin] BypassCharge repository present."
fi

# Apply Sepolicy Patches
apply_patch() {
    local target_dir="$1"
    local patch_file="$2"
    local description="$3"
    if [ -d "$target_dir" ] && [ -f "$patch_file" ]; then
        local abs_patch
        abs_patch="$(readlink -f "$patch_file")"
        if git -C "$target_dir" apply --check "$abs_patch" >/dev/null 2>&1; then
            echo "[Rodin] Applying $description to $target_dir..."
            git -C "$target_dir" apply --whitespace=nowarn "$abs_patch"
        else
            echo "[Rodin] $description already applied on $target_dir."
        fi
    fi
}

apply_patch "device/lineage/sepolicy" "device/xiaomi/rodin/patches/device_lineage_sepolicy.patch" "Sepolicy BypassCharge rules (platform_app & system_app)"
apply_patch "system/sepolicy" "device/xiaomi/rodin/patches/system_sepolicy.patch" "domain.te neverallow exemption for sysfs_batteryinfo"
