ifeq ($(TARGET_FWK_SUPPORTS_FULL_VALUEADDS),true)
ifneq ($(TARGET_IS_QLMD), true)
PRODUCT_PACKAGES += \
    QtiSoundRecorder
endif #TARGET_IS_QLMD
endif
