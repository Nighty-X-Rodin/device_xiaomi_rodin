
 
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
# Rodin-Essential: Auto-patch platform touch sepolicy on lunch
apply_rodin_sepolicy_patch() {
    local TOP="${ANDROID_BUILD_TOP:-$(gettop 2>/dev/null)}"
    if [ -z "$TOP" ]; then
        TOP="."
    fi

    local SCRIPT="$TOP/vendor/rodin-essential/tools/prepare-aosp-touch-policy.py"
    local ATTR_FILE="$TOP/system/sepolicy/public/attributes"

    if [ -f "$SCRIPT" ] && [ -f "$ATTR_FILE" ]; then
        if grep -q "rodin_native_touch_access" "$ATTR_FILE" 2>/dev/null; then
            echo "[Rodin-Essential] Platform touch sepolicy exception already applied. Ready!"
        else
            echo "[Rodin-Essential] Sourcing lunch: Applying platform touch sepolicy exception (SYS_PTRACE & DAC_OVERRIDE)..."
            python3 "$SCRIPT" "$TOP" --apply
            echo "[Rodin-Essential] Neverallow exception applied successfully!"
        fi
    fi
}

apply_rodin_sepolicy_patch
