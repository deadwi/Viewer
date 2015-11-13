// ==========================================================
// Upsampling / downsampling routine
//
// Design and implementation by
// - Herv� Drolon (drolon@infonie.fr)
// - Carsten Klein (cklein05@users.sourceforge.net)
//
// This file is part of FreeImage 3
//
// COVERED CODE IS PROVIDED UNDER THIS LICENSE ON AN "AS IS" BASIS, WITHOUT WARRANTY
// OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, WITHOUT LIMITATION, WARRANTIES
// THAT THE COVERED CODE IS FREE OF DEFECTS, MERCHANTABLE, FIT FOR A PARTICULAR PURPOSE
// OR NON-INFRINGING. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE COVERED
// CODE IS WITH YOU. SHOULD ANY COVERED CODE PROVE DEFECTIVE IN ANY RESPECT, YOU (NOT
// THE INITIAL DEVELOPER OR ANY OTHER CONTRIBUTOR) ASSUME THE COST OF ANY NECESSARY
// SERVICING, REPAIR OR CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL
// PART OF THIS LICENSE. NO USE OF ANY COVERED CODE IS AUTHORIZED HEREUNDER EXCEPT UNDER
// THIS DISCLAIMER.
//
// Use at your own risk!
// ==========================================================

#include "FreeImage_Rescale.h"
#include "FreeImage_Resize.h"

FIBITMAP * DLL_CALLCONV
FreeImage_RescaleRect(FIBITMAP *src, int dst_width, int dst_height, int src_left, int src_top, int src_right, int src_bottom, FREE_IMAGE_FILTER filter) {
	FIBITMAP *dst = NULL;

	const int src_width = FreeImage_GetWidth(src);
	const int src_height = FreeImage_GetHeight(src);

	if (!FreeImage_HasPixels(src) || (dst_width <= 0) || (dst_height <= 0) || (src_width <= 0) || (src_height <= 0)) {
		return NULL;
	}

	// normalize the rectangle
	if (src_right < src_left) {
		INPLACESWAP(src_left, src_right);
	}
	if (src_bottom < src_top) {
		INPLACESWAP(src_top, src_bottom);
	}

	// check the size of the sub image
	if((src_left < 0) || (src_right > src_width) || (src_top < 0) || (src_bottom > src_height)) {
		return NULL;
	}

	// select the filter
	CGenericFilter *pFilter = NULL;
	switch (filter) {
		case FILTER_BOX:
			pFilter = new(std::nothrow) CBoxFilter();
			break;
		case FILTER_BICUBIC:
			pFilter = new(std::nothrow) CBicubicFilter();
			break;
		case FILTER_BILINEAR:
			pFilter = new(std::nothrow) CBilinearFilter();
			break;
		case FILTER_BSPLINE:
			pFilter = new(std::nothrow) CBSplineFilter();
			break;
		case FILTER_CATMULLROM:
			pFilter = new(std::nothrow) CCatmullRomFilter();
			break;
		case FILTER_LANCZOS3:
			pFilter = new(std::nothrow) CLanczos3Filter();
			break;
	}

	if (!pFilter) {
		return NULL;
	}

	CResizeEngine2 Engine(pFilter);

	dst = Engine.scale(src, dst_width, dst_height, src_left, src_top, src_right - src_left, src_bottom - src_top, 0);
	delete pFilter;
	FreeImage_CloneMetadata(dst, src);

	return dst;
}
