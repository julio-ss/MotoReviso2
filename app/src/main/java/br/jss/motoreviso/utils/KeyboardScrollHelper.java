package br.jss.motoreviso.utils;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

import androidx.core.widget.NestedScrollView;

/**
 * Utilitário para fazer scroll automático de um campo EditText focado
 * quando o teclado virtual aparece (problema comum em Android)
 */
public class KeyboardScrollHelper {

    /**
     * Configura uma ScrollView (não-nested) para rolar automaticamente
     * o campo focado para cima quando o teclado aparece
     */
    public static void setupKeyboardScrollForScrollView(ScrollView scrollView) {
        if (scrollView == null) return;

        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    private int previousHeight = 0;

                    @Override
                    public void onGlobalLayout() {
                        int currentHeight = scrollView.getRootView().getHeight();

                        // Teclado apareceu (height diminuiu)
                        if (currentHeight < previousHeight - 100) {
                            scrollToFocusedView(scrollView);
                        }

                        previousHeight = currentHeight;
                    }
                });
    }

    /**
     * Configura uma NestedScrollView para rolar automaticamente
     * o campo focado para cima quando o teclado aparece
     */
    public static void setupKeyboardScroll(NestedScrollView scrollView) {
        if (scrollView == null) return;

        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    private int previousHeight = 0;

                    @Override
                    public void onGlobalLayout() {
                        int currentHeight = scrollView.getRootView().getHeight();

                        // Teclado apareceu (height diminuiu)
                        if (currentHeight < previousHeight - 100) {
                            scrollToFocusedView(scrollView);
                        }

                        previousHeight = currentHeight;
                    }
                });
    }

    /**
     * Faz scroll para o campo focado em ScrollView, deixando-o acima do teclado
     */
    private static void scrollToFocusedView(ScrollView scrollView) {
        View focusedView = scrollView.findFocus();
        if (focusedView == null) return;

        // Calcula a posição do campo focado
        Rect rect = new Rect();
        focusedView.getDrawingRect(rect);
        ((ViewGroup) focusedView.getParent()).offsetDescendantRectToMyCoords(focusedView, rect);

        // Scroll com delay para deixar o teclado se animar primeiro
        scrollView.postDelayed(() -> {
            int scrollY = rect.bottom - scrollView.getHeight() + 200; // 200dp de buffer
            if (scrollY > 0) {
                scrollView.smoothScrollTo(0, scrollY);
            }
        }, 100);
    }

    /**
     * Faz scroll para o campo focado em NestedScrollView, deixando-o acima do teclado
     */
    private static void scrollToFocusedView(NestedScrollView scrollView) {
        View focusedView = scrollView.findFocus();
        if (focusedView == null) return;

        // Calcula a posição do campo focado
        Rect rect = new Rect();
        focusedView.getDrawingRect(rect);
        ((ViewGroup) focusedView.getParent()).offsetDescendantRectToMyCoords(focusedView, rect);

        // Scroll com delay para deixar o teclado se animar primeiro
        scrollView.postDelayed(() -> {
            int scrollY = rect.bottom - scrollView.getHeight() + 200; // 200dp de buffer
            if (scrollY > 0) {
                scrollView.smoothScrollTo(0, scrollY);
            }
        }, 100);
    }
}
