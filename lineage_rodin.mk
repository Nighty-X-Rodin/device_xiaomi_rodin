#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from the custom device configuration.
$(call inherit-product, device/xiaomi/rodin/device.mk)

$(call inherit-product, vendor/lineage/config/common_full_phone.mk)
# Disable AVB
TARGET_AVB_DISABLE_DM_VERITY := true

PRODUCT_BRAND := Xiaomi
PRODUCT_DEVICE := rodin
PRODUCT_MANUFACTURER := Xiaomi
PRODUCT_MODEL := 2412DPC0AG
PRODUCT_NAME := lineage_rodin

PRODUCT_BRAND_FOR_ATTESTATION := $(PRODUCT_BRAND)
PRODUCT_DEVICE_FOR_ATTESTATION := $(PRODUCT_DEVICE)
PRODUCT_MODEL_FOR_ATTESTATION := $(PRODUCT_MODEL)
PRODUCT_NAME_FOR_ATTESTATION := rodin_global
PRODUCT_MANUFACTURER_FOR_ATTESTATION := $(PRODUCT_MANUFACTURER)

PRODUCT_CHARACTERISTICS := nosdcard
PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="missi-user 16 BP2A.250605.031.A3 OS3.0.301.0.WOJMIXM release-keys" \
    BuildFingerprint=POCO/rodin_global/rodin:16/BP2A.250605.031.A3/OS3.0.301.0.WOJMIXM:user/release-keys \
    DeviceName=rodin \
    DeviceProduct=rodin_global \
    SystemDevice=rodin \
    SystemName=rodin_global


# aosp flags
TARGET_HAS_UDFPS := true
TARGET_SHIPS_MIUICAMERA := true

# Axion Flags
AXION_CAMERA_REAR_INFO := 50,8
AXION_CAMERA_FRONT_INFO := 20
AXION_MAINTAINER := Nighty
AXION_PROCESSOR := Dimensity_8400U
TORCH_STR_SUPPORTED := true
TARGET_INCLUDE_AXFX := false
TARGET_ENABLE_BLUR := true
TARGET_TOUCH_BOOST_SUPPORTED := true
TARGET_INCLUDES_LOS_PREBUILTS := true
TARGET_SUPPORTED_REFRESH_RATES := 60,90,120
TARGET_NEEDS_VULKAN_MEDIA_FIX := true
PERF_GOV_SUPPORTED := true
PERF_DEFAULT_GOV := schedutil
PERF_ANIM_OVERRIDE := true
