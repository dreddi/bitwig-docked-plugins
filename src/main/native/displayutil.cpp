#include <X11/Xlib.h>
#include <iostream>
#include <memory>
#include <queue>
#include <stdexcept>

#include <jni.h>

#include "dev_late_main_DisplayUtil.h"

struct XlibErrorStatus {
	bool ok = true;
	int error_code = -1;
	std::string message;
};

static XlibErrorStatus status;

static XlibErrorStatus getStatus() {
	// clone the status
	auto stat = status;
	status.ok = true;
	status.error_code = -1;
	status.message = "";
	return stat;
}

static int xErrorHandler(Display *display, XErrorEvent *error) {
	status.ok = false;
	status.error_code = error->error_code;
	switch (status.error_code) {
		case BadValue:
			status.message = "Received BadValue error from Xlib";
			break;
		case BadWindow:
			status.message = "Received BadWindow error from Xlib";
			break;
		case BadDrawable:
			status.message = "Received BadDrawable error from Xlib";
			break;
		default:
			status.message = "Received an unknown error from Xlib";
			break;
	}

	std::cerr << status.message << std::endl;

	char buffer[1024];
	XGetErrorText(display, status.error_code, buffer, sizeof(buffer));
	std::cerr << buffer << std::endl;


	return 0;
}


// RAII style class for Xlib Display
class XlibDisplay {
public:
	XlibDisplay() {
		display = XOpenDisplay(nullptr);
		if (display == nullptr) {
			throw std::runtime_error("JNI: Unable to open X display");
		}
		oldHandler = XSetErrorHandler(xErrorHandler);
	}

	~XlibDisplay() {
		XSetErrorHandler(oldHandler);
		XCloseDisplay(display);
	}

	Display *get() { return display; }

private:
	Display *display;
	XErrorHandler oldHandler;
};


bool isWindowValid(Display *display, Window window) {

	int status;

	Window root_return;
	Window parent_return;
	Window *children_return;
	unsigned int nchildren_return;

	status = XQueryTree(display, window, &root_return, &parent_return, &children_return, &nchildren_return);
	// Some methods like XGetWindowAttributes or XGetGeometry will segfault if the given window ID
	// doesn't exist. XQueryTree will return 0 if the window ID doesn't exist.
	if (status == 0) {
		return false;
	}

	if (children_return != nullptr) {
		XFree(children_return);
	}

	return true;
}

extern "C" JNIEXPORT jint JNICALL Java_dev_late_main_DisplayUtil_moveAndResizeWindowNative(JNIEnv *env, jclass cls, jlong windowId, jint x, jint y, jint width, jint height, jboolean allowXMapWindow) {
	XlibDisplay display;

	// Cast windowId to Window
	Window window = (Window) windowId;


	// Check if the window is valid
	if (!isWindowValid(display.get(), window)) {
		return JNI_ERR;
	}


	// get window attributes
	XWindowAttributes attributes;
	XGetWindowAttributes(display.get(), window, &attributes);// synchronous
	//  XGetWindowAttributes can generate BadDrawable and BadWindow errors.
	// But it's synchronous, so the error status will be ready immediately.
	auto stat = getStatus();
	if (!stat.ok) {
		return JNI_ERR;
	}


	if (attributes.map_state == IsUnmapped && allowXMapWindow) {
		XMapWindow(display.get(), window);
	}


	//		std::cout << "moveAndResizeWindowNative: " << display.get() << ", "  << win << ", " << x << ", " << y << ", " << width << ", " << height << std::endl;
	XMoveResizeWindow(display.get(), window, x, y, width, height);

	return JNI_OK;
}


std::string get_window_name(Display *display, Window window) {
	Atom utf8_string = XInternAtom(display, "UTF8_STRING", False);
	Atom wm_name = XInternAtom(display, "WM_NAME", False);

	Atom actual_type_return;
	int actual_format_return;
	unsigned long nitems_return;
	unsigned long bytes_after_return;
	unsigned char *prop_return;

	Status status = XGetWindowProperty(display, window, wm_name, 0, (~0L),
									   False, utf8_string, &actual_type_return,
									   &actual_format_return, &nitems_return,
									   &bytes_after_return, &prop_return);

	if (status == Success && prop_return != nullptr) {
		return std::string(reinterpret_cast<char *>(prop_return));
	} else {
		return std::string();
	}
}


Window find_window_by_name(Display *display, Window root, const std::string &name) {
	// Breadth-first search, as suggested by the way the X11 window tree looks on my system.
	std::queue<Window> search_queue;
	search_queue.push(root);

	while (!search_queue.empty()) {
		Window current = search_queue.front();
		search_queue.pop();

		Window root_return, parent_return, *children;
		unsigned int num_children;
		auto status = XQueryTree(display, current, &root_return, &parent_return, &children, &num_children);

		if (status) {
			for (unsigned int i = 0; i < num_children; i++) {
				std::string window_name = get_window_name(display, children[i]);
				if (!window_name.empty()) {
					// print the window_name == name check
					//					std::cout << "window_name: " << window_name << ", name: " << name << std::endl;

					if (window_name == name) {
						//						std::cout << "found window" << std::endl;
						return children[i];
					}
				}

				// Instead of recursing, add child to the search queue
				search_queue.push(children[i]);
			}
		} else {
			// Do nothing special. We'll just let our error handler print the message
		}
	}

	return None;
}



extern "C" JNIEXPORT jintArray JNICALL Java_dev_late_main_DisplayUtil_getWindowDimensionsNative(JNIEnv *env, jclass cl, jstring windowName) {

	Window window;
	XlibDisplay display;

	const char *window_name = env->GetStringUTFChars(windowName, nullptr);
	Window root = DefaultRootWindow(display.get());

	// search for a window with a matching title
	window = find_window_by_name(display.get(), root, window_name);
	env->ReleaseStringUTFChars(windowName, window_name);

	if (window == None) {
		return nullptr;
	}


	// get window attributes
	XWindowAttributes attributes;
	XGetWindowAttributes(display.get(), window, &attributes); // synchronous
	// XGetWindowAttributes can generate BadDrawable and BadWindow errors.
	auto stat = getStatus();
	if (!stat.ok) {
		return nullptr;
	}

	// returning width and height in an int array
	jint dimensions[2] = {attributes.width, attributes.height};
	jintArray result = env->NewIntArray(2);
	env->SetIntArrayRegion(result, 0, 2, dimensions);

	return result;
}


extern "C" JNIEXPORT jlong JNICALL Java_dev_late_main_DisplayUtil_findWindowIdNative(JNIEnv *env, jclass cls, jstring windowName) {
	Window window;

	XlibDisplay display;
	const char *window_name = env->GetStringUTFChars(windowName, nullptr);
	Window root = DefaultRootWindow(display.get());
	window = find_window_by_name(display.get(), root, window_name);
	env->ReleaseStringUTFChars(windowName, window_name);

	// Window is an unsigned 32bit long, jlong is a 64bit long, so we are safe.
	return (jlong) window;
}


extern "C" JNIEXPORT jint JNICALL Java_dev_late_main_DisplayUtil_hideWindowNative(JNIEnv *env, jclass cls, jlong windowId) {
	XlibDisplay display;
	Window window = static_cast<Window>(windowId);

	if (!isWindowValid(display.get(), windowId)) {
		return false;
	}

	// get window attributes
	XWindowAttributes attributes;
	XGetWindowAttributes(display.get(), window, &attributes); // synchronous
	auto stat = getStatus();
	if (!stat.ok) {
		return JNI_ERR;
	}

	XUnmapWindow(display.get(), window); // async
	return true;
}

extern "C" JNIEXPORT jint JNICALL Java_dev_late_main_DisplayUtil_moveAndResizeWindowNative2(JNIEnv *env, jclass cls, jstring windowName, jint x, jint y, jint width, jint height, jboolean allowXMapWindow) {
	XlibDisplay display;

	// Cast windowId to Window
	Window window;
	const char *window_name = env->GetStringUTFChars(windowName, nullptr);
	Window root = DefaultRootWindow(display.get());
	window = find_window_by_name(display.get(), root, window_name);
	env->ReleaseStringUTFChars(windowName, window_name);


	// get window attributes
	XWindowAttributes attributes;
	XGetWindowAttributes(display.get(), window, &attributes); // synchronous
	//  XGetWindowAttributes can generate BadDrawable and BadWindow errors.
	auto stat = getStatus();
	if (!stat.ok) {
		return JNI_ERR;
	}


	if (attributes.map_state == IsUnmapped && allowXMapWindow) {
		XMapWindow(display.get(), window);
	}


	//		std::cout << "moveAndResizeWindowNative: " << display.get() << ", "  << win << ", " << x << ", " << y << ", " << width << ", " << height << std::endl;
	XMoveResizeWindow(display.get(), window, x, y, width, height); // async

	return JNI_OK;
}
extern "C" JNIEXPORT jint JNICALL Java_dev_late_main_DisplayUtil_hideWindowNative2(JNIEnv *env, jclass cls, jstring windowName) {
	XlibDisplay display;

	Window window;
	const char *window_name = env->GetStringUTFChars(windowName, nullptr);
	Window root = DefaultRootWindow(display.get());
	window = find_window_by_name(display.get(), root, window_name);
	env->ReleaseStringUTFChars(windowName, window_name);

	// get window attributes
	XWindowAttributes attributes;
	XGetWindowAttributes(display.get(), window, &attributes);
	auto stat = getStatus();
	if (!stat.ok) {
		return JNI_ERR;
	}

	XUnmapWindow(display.get(), window);
	return true;
}
