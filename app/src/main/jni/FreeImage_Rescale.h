// after 3.17
#include "FreeImage.h"
#include "FreeImage_Utilities.h"
#include "FreeImage_Filters.h"

FIBITMAP * DLL_CALLCONV
FreeImage_RescaleRect(FIBITMAP *src, int dst_width, int dst_height, int src_left, int src_top, int src_right, int src_bottom, FREE_IMAGE_FILTER filter);
