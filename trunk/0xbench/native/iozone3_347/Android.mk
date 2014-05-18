LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_CFLAGS:= -O3 -Dunix -DHAVE_ANSIC_C -DHAVE_PREAD -DNAME='"linux-arm"' -DLINUX_ARM -Dlinux -lrt -lpthread

LOCAL_SRC_FILES:= iozone.c libbif.c

LOCAL_MODULE:= bench_iozone_iozone
LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)

