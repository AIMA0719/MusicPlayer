package com.example.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;

import com.example.musicplayer.Activity.MainActivity;
import com.example.musicplayer.Activity.SplashActivity;

public class ToastManager extends Toast {

    Context context;
    private FrameLayout currentToastLayout = null;
    public ToastManager(Context context) {
        super(context);
        this.context = context;
    }

    public void showAnimatedToast(String message) {
        try {
            if (context != null && context instanceof Activity) {
                Activity activity = (Activity) context;

                if (!activity.isFinishing()) {
                    // 토스트 레이아웃을 생성
                    View layout = LayoutInflater.from(context).inflate(R.layout.custom_toast, null);

                    // 텍스트 설정
                    TextView text = layout.findViewById(R.id.toast_text);
                    text.setText(message);

                    // 팝업 윈도우 생성
                    PopupWindow popupWindow = new PopupWindow(
                            layout,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

                    // 투명한 배경 설정
                    popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                    popupWindow.setOutsideTouchable(true);
                    popupWindow.showAtLocation(activity.getWindow().getDecorView(), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);

                    // 아래에서 위로 올라오는 애니메이션 적용
                    View toastView = popupWindow.getContentView();
                    if (toastView != null) {
                        toastView.setTranslationY(500);

                        toastView.animate()
                                .translationYBy(-500)
                                .withEndAction(() -> {
                                    // 3초 후에 추가적인 애니메이션 적용
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        // 애니메이션이 완료된 후 팝업 닫기
                                        toastView.animate()
                                                .translationY(500)
                                                .withEndAction(popupWindow::dismiss)
                                                .start();
                                    }, 2000);
                                })
                                .start();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showAnimatedToast_real(String msg) {
        try {
            Activity activity = (Activity)context;

            if(context != null && !activity.isFinishing()) {
                removeAnimationToast(context);

                LayoutInflater inflater = LayoutInflater.from(context);
                View layout = inflater.inflate(R.layout.custom_toast, null);
                TextView text = layout.findViewById(R.id.toast_text);

                text.setText(msg);

                // Toast처럼 보이게 하기 위해 FrameLayout 사용
                final FrameLayout frameLayout = new FrameLayout(context);
                currentToastLayout = frameLayout;
                frameLayout.addView(layout);

                // 애니메이션 설정
                final Animation slideUp = AnimationUtils.loadAnimation(context, R.anim.slide_up);
                final Animation slideDown = AnimationUtils.loadAnimation(context, R.anim.slide_down);

                slideUp.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        frameLayout.postDelayed(() -> layout.startAnimation(slideDown), 2000);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });

                slideDown.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        frameLayout.removeAllViews();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });

                if(!activity.isFinishing()) {
                    // 화면에 뷰 추가
                    WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            WindowManager.LayoutParams.TYPE_APPLICATION,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                            PixelFormat.TRANSLUCENT);

                    final View activityRootView = activity.findViewById(android.R.id.content);

                    if (activityRootView != null) {
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        Rect keyboardRect = new Rect();
                        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                        Window window = activity.getWindow();
                        View activityRootView1 = window.getDecorView();
                        activityRootView1.getWindowVisibleDisplayFrame(keyboardRect);

                        int screenHeight = activityRootView.getRootView().getHeight();
                        activityRootView1.getWindowVisibleDisplayFrame(keyboardRect);

                        window.getDecorView().getWindowVisibleDisplayFrame(keyboardRect);
                        int keypadHeight1 = screenHeight - keyboardRect.bottom;

                        if (keypadHeight1 > screenHeight * 0.15) {  // 키보드가 뜬 경우
                            int yOffset = Math.round(UtilManager.convertDpToPixel(85));
                            params.y = screenHeight - (keypadHeight1 + yOffset);  // 키보드 높이와 yOffset을 더한 값
                            params.gravity = Gravity.TOP;
                        } else {  // 키보드가 내려간 경우
                            params.y = 0;
                            params.gravity = Gravity.BOTTOM;
                        }

                        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                        if(wm != null){
                            wm.addView(frameLayout, params);
                        }

                        // slide_up 애니메이션 시작
                        layout.startAnimation(slideUp);

                        if (activity instanceof SplashActivity) {
                            ((SplashActivity) activity).getOnBackPressedDispatcher().
                                    addCallback(((SplashActivity) activity), new OnBackPressedCallback(true) {
                                        @Override
                                        public void handleOnBackPressed() {
                                            removeAnimationToast(); // 커스텀 토스트 해제
                                            ((SplashActivity) activity).onBackPressed(); // 액티비티 종료
                                        }
                                    });
                        } else if (activity instanceof MainActivity) {
                            ((MainActivity) activity).getOnBackPressedDispatcher().
                                    addCallback(((MainActivity) activity), new OnBackPressedCallback(true) {
                                        @Override
                                        public void handleOnBackPressed() {
                                            removeAnimationToast(); // 커스텀 토스트 해제
                                            ((MainActivity) activity).onBackPressed(); // 액티비티 종료
                                        }
                                    });
                        }
                    }
                }
            }
        } catch (Exception e) {
            removeAnimationToast();
            e.printStackTrace();
        }
    }

    public void removeAnimationToast(Context... contexts) {
        try {
            if(currentToastLayout != null && currentToastLayout.getParent() != null && contexts.length != 0) {
                WindowManager wm = null;
                if(contexts[0] == null){
                    wm = (WindowManager) ContextManager.getMainContext().getSystemService(Context.WINDOW_SERVICE);
                }else {
                    if(contexts[0] instanceof MainActivity){
                        if(ContextManager.getMainContext() != null){
                            wm = (WindowManager) ContextManager.getMainContext().getSystemService(Context.WINDOW_SERVICE);
                        }
                    }else {
                        wm = (WindowManager) contexts[0].getSystemService(Context.WINDOW_SERVICE);
                    }
                }
                if(wm != null){
                    wm.removeView(currentToastLayout);
                }
                currentToastLayout = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
