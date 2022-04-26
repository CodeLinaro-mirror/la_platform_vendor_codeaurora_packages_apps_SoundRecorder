ifneq ($(TARGET_1G_DDR_RAM), true)
ifeq ($(TARGET_FWK_SUPPORTS_FULL_VALUEADDS),true)
PRODUCT_PACKAGES += \
    QtiSoundRecorder
endif
endif
