// winApp.cpp : Defines the entry point for the application.
//

#include "framework.h"
#include "winApp.h"

#include <iostream>
#include <string>
#include <sstream>
#include <cwchar>
#include <cstring>
#include <chrono>
#include <inttypes.h>

#include <DbgHelp.h>

#include "libKmpWebrtc.h"

#define MAX_LOADSTRING 100

// Global Variables:
HINSTANCE hInst;                                // current instance
WCHAR szTitle[MAX_LOADSTRING];                  // The title bar text
WCHAR szWindowClass[MAX_LOADSTRING];            // the main window class name

HWND gHwnd;
HWND loopbackButton;

HWND g_renderers_hwnd = nullptr;
WNDCLASSEXW g_renderers_wcex;

void* gPcClientFactory = nullptr;
void* gPcClient = nullptr;
PCClientCallback* gPcClientCallback = nullptr;
void* gRemoteRenderer = nullptr;

#pragma comment( lib, "DbgHelp" )

LONG WINAPI unhandled_exception_filter(struct _EXCEPTION_POINTERS* exception_point)
{

    char buf[1024];
    int64_t now = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();
    snprintf(buf, 1023, "winApp-%" PRId64 ".dmp", now);
    std::string dumpName(buf);
    std::wstring wDumpName(dumpName.begin(), dumpName.end());
    HANDLE dump_file = CreateFile(wDumpName.c_str(), GENERIC_WRITE, 0,
        NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    if (dump_file)
    {
        MINIDUMP_EXCEPTION_INFORMATION dump_exception;

        dump_exception.ExceptionPointers = exception_point;
        dump_exception.ThreadId = GetCurrentThreadId();
        dump_exception.ClientPointers = TRUE;

        MiniDumpWriteDump(GetCurrentProcess(), GetCurrentProcessId(),
            dump_file, MiniDumpNormal, &dump_exception, NULL, NULL);

        CloseHandle(dump_file);
        dump_file = NULL;
    }

    return EXCEPTION_EXECUTE_HANDLER;
}

LRESULT CALLBACK ChildWndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
    return DefWindowProc(hWnd, message, wParam, lParam);
}

ATOM RegisterChildClass(HINSTANCE hInstance, WNDCLASSEXW* wcex, LPCWSTR className)
{
    if (!wcex)
        return -1;

    ZeroMemory(wcex, sizeof(WNDCLASSEXW));
    wcex->cbClsExtra = NULL;
    wcex->cbSize = sizeof(WNDCLASSEXW);
    wcex->cbWndExtra = NULL;
    wcex->hbrBackground = (HBRUSH)COLOR_WINDOW;
    wcex->hCursor = LoadCursor(nullptr, IDC_ARROW);
    wcex->hIcon = NULL;
    wcex->hIconSm = NULL;
    wcex->hInstance = hInstance;
    wcex->lpfnWndProc = ChildWndProc;
    wcex->lpszClassName = className;
    wcex->lpszMenuName = NULL;
    wcex->style = CS_HREDRAW | CS_VREDRAW;

    ATOM ret = RegisterClassEx(wcex);
    if (!ret)
    {
        int nResult = GetLastError();
        MessageBox(NULL, L"Window class creation failed", L"Window Class Failed", MB_ICONERROR);
    }

    return ret;
}

// Forward declarations of functions included in this code module:
ATOM                MyRegisterClass(HINSTANCE hInstance);
BOOL                InitInstance(HINSTANCE, int);
LRESULT CALLBACK    WndProc(HWND, UINT, WPARAM, LPARAM);
INT_PTR CALLBACK    About(HWND, UINT, WPARAM, LPARAM);

int APIENTRY wWinMain(_In_ HINSTANCE hInstance,
                     _In_opt_ HINSTANCE hPrevInstance,
                     _In_ LPWSTR    lpCmdLine,
                     _In_ int       nCmdShow)
{
    UNREFERENCED_PARAMETER(hPrevInstance);
    UNREFERENCED_PARAMETER(lpCmdLine);
    SetUnhandledExceptionFilter(unhandled_exception_filter);

#if 0
    AllocConsole();
    freopen("conout$", "w", stdout);
    freopen("conout$", "w", stderr);
#endif

    // TODO: Place code here.

    // Initialize global strings
    LoadStringW(hInstance, IDS_APP_TITLE, szTitle, MAX_LOADSTRING);
    LoadStringW(hInstance, IDC_WINAPP, szWindowClass, MAX_LOADSTRING);
    MyRegisterClass(hInstance);

    // Perform application initialization:
    if (!InitInstance (hInstance, nCmdShow))
    {
        return FALSE;
    }

    RegisterChildClass(hInstance, &g_renderers_wcex, L"renderers_wcex");
    HACCEL hAccelTable = LoadAccelerators(hInstance, MAKEINTRESOURCE(IDC_WINAPP));

    MSG msg;

    // Main message loop:
    while (GetMessage(&msg, nullptr, 0, 0))
    {
        if (!TranslateAccelerator(msg.hwnd, hAccelTable, &msg))
        {
            TranslateMessage(&msg);
            DispatchMessage(&msg);
        }
    }

    return (int) msg.wParam;
}



//
//  FUNCTION: MyRegisterClass()
//
//  PURPOSE: Registers the window class.
//
ATOM MyRegisterClass(HINSTANCE hInstance)
{
    WNDCLASSEXW wcex;

    wcex.cbSize = sizeof(WNDCLASSEX);

    wcex.style          = CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc    = WndProc;
    wcex.cbClsExtra     = 0;
    wcex.cbWndExtra     = 0;
    wcex.hInstance      = hInstance;
    wcex.hIcon          = LoadIcon(hInstance, MAKEINTRESOURCE(IDI_WINAPP));
    wcex.hCursor        = LoadCursor(nullptr, IDC_ARROW);
    wcex.hbrBackground  = (HBRUSH)(COLOR_WINDOW+1);
    wcex.lpszMenuName   = MAKEINTRESOURCEW(IDC_WINAPP);
    wcex.lpszClassName  = szWindowClass;
    wcex.hIconSm        = LoadIcon(wcex.hInstance, MAKEINTRESOURCE(IDI_SMALL));

    return RegisterClassExW(&wcex);
}

//
//   FUNCTION: InitInstance(HINSTANCE, int)
//
//   PURPOSE: Saves instance handle and creates main window
//
//   COMMENTS:
//
//        In this function, we save the instance handle in a global variable and
//        create and display the main program window.
//
BOOL InitInstance(HINSTANCE hInstance, int nCmdShow)
{
   hInst = hInstance; // Store instance handle in our global variable

   gHwnd = CreateWindowW(szWindowClass, szTitle, WS_OVERLAPPEDWINDOW,
      CW_USEDEFAULT, 0, CW_USEDEFAULT, 0, nullptr, nullptr, hInstance, nullptr);

   if (!gHwnd)
   {
      return FALSE;
   }

   ShowWindow(gHwnd, nCmdShow);
   UpdateWindow(gHwnd);

   return TRUE;
}

static void pcClientFactoryErrorHandler(void*, int error, const char* message) {
    std::ostringstream oss;
    oss << "pcClientFactoryError code: " << error << ", message: " << message;
    std::string str = oss.str();
    std::wstring wstr(str.begin(), str.end());
    ATOM atom = GlobalAddAtom(wstr.c_str());
    PostMessage(gHwnd, IDM_ERROR_MESSAGE, WPARAM(atom), 0);
}

static const char* pcClientOnPreferCodecs(void*, const char* peer_uid, const char* sdp) {
    return sdp;
}

static void pcClientOnPeerConnectionStatsReady(void*, const char* peer_uid, const char* stats) {
    std::ostringstream oss;
    oss << "pcClientOnPeerConnectionStatsReady " << peer_uid << " " << stats;
    LogInfo(oss.str().c_str());
}

static void pcClientOnIceDisconnected(void*, const char* peer_uid) {
}

static void pcClientOnError(void*, const char* peer_uid, int code) {
    std::ostringstream oss;
    oss << "pcClientError code: " << code;
    std::string str = oss.str();
    std::wstring wstr(str.begin(), str.end());
    ATOM atom = GlobalAddAtom(wstr.c_str());
    PostMessage(gHwnd, IDM_ERROR_MESSAGE, WPARAM(atom), 0);
}

static void pcClientOnLocalDescription(void*, const char* peer_uid, int type, const char* sdp);
static void pcClientOnIceCandidate(void*, const char* peer_uid, const char* sdp_mid, int m_line_index, const char* sdp);
static void pcClientOnIceConnected(void*, const char* peer_uid);

static void startLoopback() {
    // 1. initialize
    InitializeWebRTC("", true);

    // 2. create PcClientFactory
    PCClientFactoryConfig* config = DefaultPCClientFactoryConfig();
    //config->video_capture_impl = kKmpWebRTCCaptureSystemCamera;
    config->video_capture_impl = kKmpWebRTCCaptureScreen;
    config->private_config.hwnd = g_renderers_hwnd;
    config->private_config.disable_encryption = 1;
    gPcClientFactory = CreatePCClientFactory(config, pcClientFactoryErrorHandler, gHwnd);
    PCClientFactoryConfigDestroy(&config);

    // 3. create local tracks
    CreateLocalTracks(gPcClientFactory);

    // 4. add local preview & start camera capture
    StartVideoCapture(gPcClientFactory);

    // 5. create PcClient
    gPcClientCallback = new PCClientCallback();
    gPcClientCallback->on_prefer_codecs = pcClientOnPreferCodecs;
    gPcClientCallback->on_local_description = pcClientOnLocalDescription;
    gPcClientCallback->on_ice_candidate = pcClientOnIceCandidate;
    gPcClientCallback->on_stats_ready = pcClientOnPeerConnectionStatsReady;
    gPcClientCallback->on_ice_connected = pcClientOnIceConnected;
    gPcClientCallback->on_ice_disconnected = pcClientOnIceDisconnected;
    gPcClientCallback->on_error = pcClientOnError;
    gPcClient = CreatePeerConnectionClient(gPcClientFactory, "test", kKmpWebRTCDirSendRecv, 1, 800, 30, gPcClientCallback, gHwnd);

    // 6. create pc
    CreatePeerConnection(gPcClient);

    // 7. create offer
    CreateOffer(gPcClient);
}

static void pcClientOnLocalDescription(void*, const char* peer_uid, int type, const char* sdp) {
    std::ostringstream oss;
    oss << "pcClientOnLocalDescription " << peer_uid << " " << sdp;
    LogInfo(oss.str().c_str());
    // 8. send offer to remote, get answer from remote, and set answer
    SetRemoteDescription(gPcClient, kKmpWebRTCSdpAnswer, sdp);
}

static void pcClientOnIceCandidate(void*, const char* peer_uid, const char* sdp_mid, int m_line_index, const char* sdp) {
    std::ostringstream oss;
    oss << "pcClientOnIceCandidate " << peer_uid << " " << sdp;
    LogInfo(oss.str().c_str());
    // 9. send ice candidate to remote, get ice candidate from remote, add ice candidate
    AddIceCandidate(gPcClient, sdp_mid, m_line_index, sdp);
}

static void pcClientOnIceConnected(void*, const char* peer_uid) {
    std::ostringstream oss;
    oss << "pcClientOnIceConnected " << peer_uid;
    LogInfo(oss.str().c_str());
    SetTimer(gHwnd, IDT_GET_STATS, 5000, NULL);

    // 10. on ice connected, add renderer for remote stream
    AddRemoteTrackRenderer(gPcClient, gRemoteRenderer);
}

static void stopLoopback() {
    KillTimer(gHwnd, IDT_GET_STATS);
    StopVideoCapture(gPcClientFactory);
    ClosePeerConnectionClient(&gPcClient);
    DestroyPCClientFactory(&gPcClientFactory);
    PCClientVideoRendererDestroy(gRemoteRenderer);
    gRemoteRenderer = nullptr;
}

//
//  FUNCTION: WndProc(HWND, UINT, WPARAM, LPARAM)
//
//  PURPOSE: Processes messages for the main window.
//
//  WM_COMMAND  - process the application menu
//  WM_PAINT    - Paint the main window
//  WM_DESTROY  - post a quit message and return
//
//
LRESULT CALLBACK WndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
    switch (message)
    {
    case WM_CREATE:
    {
        loopbackButton = CreateWindow(
            TEXT("button"), TEXT("LOOPBACK"),
            WS_CHILD | WS_VISIBLE,
            200, 10, 75, 20,
            hWnd, (HMENU)IDB_BUTTON_LOOPBACK,
            hInst, NULL
        );
        break;
    }
    case WM_TIMER: {
        int wmId = LOWORD(wParam);
        if (wmId == IDT_GET_STATS) {
            GetStats(gPcClient);
        }
        break;
    }
    case WM_COMMAND:
        {
            int wmId = LOWORD(wParam);
            // Parse the menu selections:
            switch (wmId)
            {
            case IDB_BUTTON_LOOPBACK:
            {
                if (gPcClientFactory == nullptr) {
                    SetWindowText(loopbackButton, TEXT("STOP"));

                    RECT wSize;
                    GetWindowRect(gHwnd, &wSize);
                    int marginTop = 50;
                    int fs_width = wSize.right - wSize.left;
                    int fs_height = wSize.bottom - wSize.top - marginTop;
                    g_renderers_hwnd = CreateWindowEx(NULL,
                    	g_renderers_wcex.lpszClassName,
                    	L"Child Window", WS_CHILD | WS_VISIBLE,
                    	0, marginTop, fs_width, fs_height,
                        gHwnd, NULL, hInst, NULL);
                    
                    gRemoteRenderer = PCClientVideoRendererCreate(0, 0, fs_width, fs_height, 0, 2 /* center crop */);

                    startLoopback();
                } else {
                    SetWindowText(loopbackButton, TEXT("LOOPBACK"));
                    stopLoopback();
                }
                break;
            }
            case IDM_ABOUT:
                DialogBox(hInst, MAKEINTRESOURCE(IDD_ABOUTBOX), hWnd, About);
                break;
            case IDM_EXIT:
                DestroyWindow(hWnd);
                break;
            default:
                return DefWindowProc(hWnd, message, wParam, lParam);
            }
        }
        break;
    case IDM_ERROR_MESSAGE: {
        ATOM atom = (ATOM)wParam;
        wchar_t buffer[1024];
        GlobalGetAtomName(atom, buffer, sizeof(buffer));
        MessageBox(gHwnd, buffer, L"Error", MB_OK | MB_ICONINFORMATION);
        GlobalDeleteAtom(atom);
        break;
    }
    case WM_PAINT:
        {
            PAINTSTRUCT ps;
            HDC hdc = BeginPaint(hWnd, &ps);
            // TODO: Add any drawing code that uses hdc here...
            EndPaint(hWnd, &ps);
        }
        break;
    case WM_DESTROY:
        if (gPcClientFactory != nullptr) {
            stopLoopback();
        }
        PostQuitMessage(0);
        break;
    default:
        return DefWindowProc(hWnd, message, wParam, lParam);
    }
    return 0;
}

// Message handler for about box.
INT_PTR CALLBACK About(HWND hDlg, UINT message, WPARAM wParam, LPARAM lParam)
{
    UNREFERENCED_PARAMETER(lParam);
    switch (message)
    {
    case WM_INITDIALOG:
        return (INT_PTR)TRUE;

    case WM_COMMAND:
        if (LOWORD(wParam) == IDOK || LOWORD(wParam) == IDCANCEL)
        {
            EndDialog(hDlg, LOWORD(wParam));
            return (INT_PTR)TRUE;
        }
        break;
    }
    return (INT_PTR)FALSE;
}
