package com.fongmi.android.tv.lab;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.FocusFinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;

/** Runtime view and key-event checks for the production Lab command list and WebView panel. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = App.class, qualifiers = "w800dp-h450dp-land")
public class LabUiDeviceFailureFollowupTest {

    public static class Host extends AppCompatActivity {
        RecyclerView recycler;
        NestedScrollView scrollView;
        LabCommandAdapter adapter;
        LabModels.Item item;
        int opened;
        int acted;
        Intent startedIntent;

        @Override
        public void startActivity(Intent intent) {
            startedIntent = intent;
        }

        @Override
        protected void onCreate(Bundle state) {
            setTheme(R.style.Theme_App);
            super.onCreate(state);
            setContentView(R.layout.activity_lab_detail);
            recycler = findViewById(R.id.commandRecycler);
            scrollView = findViewById(R.id.scrollView);
            recycler.setLayoutManager(new LinearLayoutManager(this));
            adapter = new LabCommandAdapter(this, new LabCommandAdapter.Listener() {
                @Override
                public void onOpen(LabModels.Item item, LabModels.Command command) {
                    opened++;
                }

                @Override
                public void onAction(LabModels.Item item, LabModels.Command command) {
                    acted++;
                }

                @Override
                public void onLongPress(LabModels.Item item, LabModels.Command command) {
                }
            });
            recycler.setAdapter(adapter);
            item = itemWithCommands(12);
            adapter.setItem(item);
        }

        void layoutWindow() {
            View decor = getWindow().getDecorView();
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            decor.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            decor.layout(0, 0, width, height);
        }
    }

    @Test
    public void commandRowsUseTwoLevelDpadNavigationAndKeepClicksIndependent() {
        try (var hostController = Robolectric.buildActivity(Host.class).setup()) {
            Host host = hostController.get();
            host.layoutWindow();

            RecyclerView.ViewHolder first = host.recycler.findViewHolderForAdapterPosition(0);
            RecyclerView.ViewHolder second = host.recycler.findViewHolderForAdapterPosition(1);
            assertTrue("the production detail layout must attach the command rows", first != null && second != null);
            View row = first.itemView;
            View action = row.findViewById(R.id.btnAction);
            assertFalse("status markers must not be focus stops", row.findViewById(R.id.statusDot).isFocusable());
            assertFalse("the command label must not be a separate focus stop", row.findViewById(R.id.name).isFocusable());
            assertFalse("the running tag must not be a separate focus stop", row.findViewById(R.id.runningTag).isFocusable());

            // Robolectric starts activities in touch mode; TV navigation is represented by making
            // the production focus targets eligible for programmatic focus in this test host.
            row.setFocusableInTouchMode(true);
            action.setFocusableInTouchMode(true);
            second.itemView.setFocusableInTouchMode(true);
            second.itemView.findViewById(R.id.btnAction).setFocusableInTouchMode(true);
            TextView extraAction = new TextView(host);
            extraAction.setText("More");
            extraAction.setFocusable(true);
            extraAction.setFocusableInTouchMode(true);
            extraAction.setClickable(true);
            extraAction.setOnClickListener(v -> host.acted++);
            ((LinearLayout) row).addView(extraAction,
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(host, 32)));
            host.adapter.onBindViewHolder((LabCommandAdapter.Holder) first, 0);
            assertTrue(first.itemView.requestFocus());
            assertTrue("initial focus belongs to the command row", row.isFocused());

            sendDpad(row, KeyEvent.KEYCODE_DPAD_RIGHT);
            assertTrue("RIGHT must enter this row's action control", action.isFocused());
            assertFalse("only the row body, not both focus levels, may draw focus", row.isFocused());
            sendDpad(action, KeyEvent.KEYCODE_DPAD_RIGHT);
            assertTrue("RIGHT moves between multiple inline controls", extraAction.isFocused());
            sendDpad(extraAction, KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals("ENTER on the action invokes only the row action", 1, host.acted);
            assertEquals("ENTER on the action must not also open command details", 0, host.opened);

            sendDpad(extraAction, KeyEvent.KEYCODE_DPAD_LEFT);
            assertTrue("LEFT moves from the second inline control to the first", action.isFocused());
            sendDpad(action, KeyEvent.KEYCODE_DPAD_LEFT);
            assertTrue("LEFT from the first inline control returns to the row", row.isFocused());
            sendDpad(row, KeyEvent.KEYCODE_DPAD_RIGHT);
            sendDpad(action, KeyEvent.KEYCODE_DPAD_DOWN);
            assertTrue("DOWN from a row advances to the next row body", second.itemView.isFocused());
            sendDpad(second.itemView, KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals("ENTER on the row body opens details", 1, host.opened);
            assertEquals("opening details does not execute the action", 1, host.acted);

            assertTrue("touch on a row still opens details", first.itemView.performClick());
            assertTrue("touch on the action still invokes its own listener", action.performClick());
            assertEquals(2, host.opened);
            assertEquals(2, host.acted);
        }
    }

    @Test
    public void repeatedDpadDownKeepsTheFocusedCommandVisibleInLandscape() {
        try (var hostController = Robolectric.buildActivity(Host.class).setup()) {
            Host host = hostController.get();
            host.layoutWindow();
            host.scrollView.scrollTo(0, 0);

            RecyclerView.ViewHolder first = host.recycler.findViewHolderForAdapterPosition(0);
            assertTrue("the first command must be present in the production list", first != null);
            for (int i = 0; i < host.recycler.getAdapter().getItemCount(); i++) {
                RecyclerView.ViewHolder row = host.recycler.findViewHolderForAdapterPosition(i);
                assertTrue("command row " + i + " must be attached", row != null);
                row.itemView.setFocusableInTouchMode(true);
                row.itemView.findViewById(R.id.btnAction).setFocusableInTouchMode(true);
            }
            assertTrue(first.itemView.requestFocus());
            for (int i = 0; i < 11; i++) {
                RecyclerView.ViewHolder current = host.recycler.findViewHolderForAdapterPosition(i);
                RecyclerView.ViewHolder next = host.recycler.findViewHolderForAdapterPosition(i + 1);
                sendDpad(current.itemView, KeyEvent.KEYCODE_DPAD_DOWN);
                assertTrue("DOWN must land on row " + (i + 1) + " body", next.itemView.isFocused());
            }
            RecyclerView.ViewHolder last = host.recycler.findViewHolderForAdapterPosition(11);
            assertTrue("DPAD movement must scroll the nested page", host.scrollView.getScrollY() > 0);
            host.layoutWindow();

            android.graphics.Rect viewport = new android.graphics.Rect();
            android.graphics.Rect row = new android.graphics.Rect();
            assertTrue(host.scrollView.getGlobalVisibleRect(viewport));
            assertTrue(last.itemView.getGlobalVisibleRect(row));
            assertTrue("focus must keep the full row inside the visible viewport", row.height() == last.itemView.getHeight()
                    && row.top >= viewport.top && row.bottom <= viewport.bottom);
        }
    }

    @Test
    public void closingCommandDetailsRestoresTheOriginalRowFocus() {
        try (var hostController = Robolectric.buildActivity(Host.class).setup()) {
            Host host = hostController.get();
            host.layoutWindow();
            RecyclerView.ViewHolder first = host.recycler.findViewHolderForAdapterPosition(0);
            assertTrue(first != null);
            first.itemView.setFocusableInTouchMode(true);
            assertTrue(first.itemView.requestFocus());

            new LabCommandSheet(host, host.item, host.item.commands.get(0), null).show();
            Dialog details = ShadowDialog.getLatestDialog();
            assertTrue("the production command detail sheet must be shown", details != null && details.isShowing());
            details.onBackPressed();
            assertFalse("BACK closes the parent command sheet", details.isShowing());
            host.layoutWindow();
            assertTrue("closing details must restore focus to the originating command row", first.itemView.isFocused());
        }
    }

    @Test
    public void commandSheetHeaderCloseIsReachableByDpadAndDoesNotRunCommand() {
        try (var hostController = Robolectric.buildActivity(Host.class).setup()) {
            Host host = hostController.get();
            host.layoutWindow();
            RecyclerView.ViewHolder first = host.recycler.findViewHolderForAdapterPosition(0);
            assertTrue(first != null);
            first.itemView.setFocusableInTouchMode(true);
            assertTrue(first.itemView.requestFocus());

            new LabCommandSheet(host, host.item, host.item.commands.get(0), null).show();
            BottomSheetDialog details = (BottomSheetDialog) ShadowDialog.getLatestDialog();
            assertTrue("the production command detail sheet must be shown", details != null && details.isShowing());
            View close = details.findViewById(R.id.sheetBtnClose);
            assertTrue("the shared sheet must expose a focusable title-bar close control",
                    close != null && close.isFocusable() && close.isClickable());
            close.setFocusableInTouchMode(true);
            assertTrue(close.requestFocus());
            sendDpad(close, KeyEvent.KEYCODE_DPAD_CENTER);
            assertFalse("confirm on close dismisses the sheet", details.isShowing());
            assertEquals("closing the sheet must not run a command", 0, host.acted);
            assertEquals("closing the sheet must not open another details view", 0, host.opened);
            host.layoutWindow();
            assertTrue("the originating command row regains focus", first.itemView.isFocused());
        }
    }

    @Test
    public void commandSheetDpadCanEnterMihomoParametersReachActionsAndReturn() {
        try (var hostController = Robolectric.buildActivity(Host.class).setup()) {
            Host host = hostController.get();
            host.layoutWindow();
            LabModels.Item item = itemWithCommands(1);
            LabModels.Command command = item.commands.get(0);
            command.name = "启动代理";
            command.command = "mihomo -f {config_path} -ext-ctl {ui_host} -secret {secret}";
            command.variables = new ArrayList<>();
            command.variables.add(variable("config_path", "配置文件", "file", "config.yaml"));
            command.variables.add(variable("ui_host", "外部服务", "text", "0.0.0.0:9090"));
            command.variables.add(variable("secret", "服务密码", "text", "123456"));

            new LabCommandSheet(host, item, command, null).show();
            BottomSheetDialog details = (BottomSheetDialog) ShadowDialog.getLatestDialog();
            assertTrue(details != null && details.isShowing());
            View focusRoot = layoutDialog(details, host);
            View close = details.findViewById(R.id.sheetBtnClose);
            LinearLayout variables = details.findViewById(R.id.sheetVariables);
            ArrayList<View> inputs = new ArrayList<>();
            for (int i = 0; i < variables.getChildCount(); i++) {
                View input = variables.getChildAt(i).findViewById(R.id.input);
                assertTrue("every dynamic Mihomo parameter must have a production input", input != null);
                input.setFocusableInTouchMode(true);
                inputs.add(input);
            }
            View edit = details.findViewById(R.id.sheetBtnEdit);
            close.setFocusableInTouchMode(true);
            edit.setFocusableInTouchMode(true);
            assertTrue(close.requestFocus());

            moveFocusByDpad(focusRoot, close, View.FOCUS_DOWN);
            assertSameFocus(focusRoot, inputs.get(0), "DOWN from close enters the first dynamic parameter");
            moveFocusByDpad(focusRoot, inputs.get(0), View.FOCUS_DOWN);
            assertSameFocus(focusRoot, inputs.get(1), "DOWN advances to the next parameter");
            moveFocusByDpad(focusRoot, inputs.get(1), View.FOCUS_DOWN);
            assertSameFocus(focusRoot, inputs.get(2), "DOWN advances to the last Mihomo parameter");

            android.widget.EditText password = (android.widget.EditText) inputs.get(2);
            password.setText("edited-password");
            assertTrue("editing a parameter still updates the command preview",
                    ((android.widget.EditText) details.findViewById(R.id.sheetCommandPreview))
                            .getText().toString().contains("edited-password"));
            password.setSelection(4);
            assertEquals("the text editor retains its insertion point before navigation", 4, password.getSelectionStart());
            assertEquals("the production field keeps its original text input mode",
                    android.text.InputType.TYPE_CLASS_TEXT, password.getInputType());
            moveFocusByDpad(focusRoot, password, View.FOCUS_DOWN);
            assertEquals("moving focus does not reset the parameter editor insertion point", 4, password.getSelectionStart());
            View preview = details.findViewById(R.id.sheetCommandPreview);
            assertSameFocus(focusRoot, preview, "DOWN from the last parameter reaches the command preview");
            moveFocusByDpad(focusRoot, preview, View.FOCUS_DOWN);
            assertSameFocus(focusRoot, edit, "DOWN from the command preview reaches the bottom action area");
            moveFocusByDpad(focusRoot, edit, View.FOCUS_UP);
            assertSameFocus(focusRoot, preview, "UP from the bottom action returns through the command preview");
            moveFocusByDpad(focusRoot, preview, View.FOCUS_UP);
            assertSameFocus(focusRoot, password, "UP from the preview returns to the last parameter");
            moveFocusByDpad(focusRoot, password, View.FOCUS_UP);
            moveFocusByDpad(focusRoot, inputs.get(1), View.FOCUS_UP);
            assertSameFocus(focusRoot, inputs.get(0), "UP moves back through the earlier parameters");
            moveFocusByDpad(focusRoot, inputs.get(0), View.FOCUS_UP);
            assertSameFocus(focusRoot, close, "UP from the first parameter returns to the close control");

            details.dismiss();
        }
    }

    @Test
    public void commandSheetLongDynamicParameterListRemainsFocusableAndScrolls() {
        try (var hostController = Robolectric.buildActivity(Host.class).setup()) {
            Host host = hostController.get();
            LabModels.Item item = itemWithCommands(1);
            LabModels.Command command = item.commands.get(0);
            command.command = "run {value0}";
            command.variables = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                command.variables.add(variable("value" + i, "参数 " + i, "text", "v" + i));
            }

            new LabCommandSheet(host, item, command, null).show();
            BottomSheetDialog details = (BottomSheetDialog) ShadowDialog.getLatestDialog();
            View focusRoot = layoutDialog(details, host);
            View close = details.findViewById(R.id.sheetBtnClose);
            LinearLayout variables = details.findViewById(R.id.sheetVariables);
            ArrayList<View> inputs = new ArrayList<>();
            for (int i = 0; i < variables.getChildCount(); i++) {
                View input = variables.getChildAt(i).findViewById(R.id.input);
                input.setFocusableInTouchMode(true);
                inputs.add(input);
            }
            close.setFocusableInTouchMode(true);
            assertTrue(close.requestFocus());
            moveFocusByDpad(focusRoot, close, View.FOCUS_DOWN);
            assertSameFocus(focusRoot, inputs.get(0), "long dynamic list begins at its first parameter");
            for (int i = 0; i < inputs.size() - 1; i++) {
                moveFocusByDpad(focusRoot, inputs.get(i), View.FOCUS_DOWN);
                assertSameFocus(focusRoot, inputs.get(i + 1), "DPAD reaches dynamic parameter " + (i + 1));
                ShadowLooper.idleMainLooper();
            }
            NestedScrollView scroll = details.findViewById(R.id.sheetScroll);
            assertTrue("focused long-list input must scroll into the visible viewport; scrollY=" + scroll.getScrollY()
                            + " viewport=" + scroll.getWidth() + "x" + scroll.getHeight()
                            + " content=" + (scroll.getChildAt(0) == null ? "null" : scroll.getChildAt(0).getWidth() + "x" + scroll.getChildAt(0).getHeight())
                            + " dialog=" + focusRoot.getMeasuredWidth() + "x" + focusRoot.getMeasuredHeight(),
                    scroll.getScrollY() > 0);
            android.graphics.Rect visible = new android.graphics.Rect();
            assertTrue(focusRoot.findFocus().getGlobalVisibleRect(visible));
            assertTrue("last dynamic parameter retains a visible rectangle", visible.height() > 0);
            details.dismiss();

            command.variables.clear();
            command.variables.add(variable("new_first", "新参数一", "text", "a"));
            command.variables.add(variable("new_second", "新参数二", "text", "b"));
            new LabCommandSheet(host, item, command, null).show();
            BottomSheetDialog updated = (BottomSheetDialog) ShadowDialog.getLatestDialog();
            View updatedRoot = layoutDialog(updated, host);
            View updatedClose = updated.findViewById(R.id.sheetBtnClose);
            LinearLayout updatedVariables = updated.findViewById(R.id.sheetVariables);
            View updatedFirst = updatedVariables.getChildAt(0).findViewById(R.id.input);
            View updatedSecond = updatedVariables.getChildAt(1).findViewById(R.id.input);
            updatedFirst.setFocusableInTouchMode(true);
            updatedSecond.setFocusableInTouchMode(true);
            updatedClose.setFocusableInTouchMode(true);
            assertTrue("a newly rendered dynamic parameter list remains reachable", updatedClose.requestFocus());
            moveFocusByDpad(updatedRoot, updatedClose, View.FOCUS_DOWN);
            assertSameFocus(updatedRoot, updatedFirst, "re-rendered list starts at its new first parameter");
            moveFocusByDpad(updatedRoot, updatedFirst, View.FOCUS_DOWN);
            assertSameFocus(updatedRoot, updatedSecond, "re-rendered list uses its updated parameter order");
            updated.dismiss();
        }
    }

    @Test
    public void backDismissesOnlyTheTopProductionLabDialog() {
        try (var hostController = Robolectric.buildActivity(Host.class).setup()) {
            Host host = hostController.get();
            host.layoutWindow();
            LabModels.Command command = host.item.commands.get(0);
            new LabCommandSheet(host, host.item, command, null).show();
            BottomSheetDialog parent = (BottomSheetDialog) ShadowDialog.getLatestDialog();
            assertTrue(parent != null && parent.isShowing());

            View edit = parent.findViewById(R.id.sheetBtnEdit);
            assertTrue(edit.performClick());
            Dialog editDialog = ShadowDialog.getLatestDialog();
            assertTopBackDismisses(editDialog, parent, "command edit");

            LabModels.Variable variable = new LabModels.Variable();
            variable.key = "sample";
            variable.name = "Sample";
            variable.type = "text";
            LabVariableDialog.show(host, "Parameters", java.util.Collections.singletonList(variable), values -> {});
            assertTopBackDismisses(ShadowDialog.getLatestDialog(), parent, "variable editor");

            new LabFileBrowserDialog(host, true, path -> {}).show();
            assertTopBackDismisses(ShadowDialog.getLatestDialog(), parent, "file browser");

            LabRunningDialog.show(host);
            assertTopBackDismisses(ShadowDialog.getLatestDialog(), parent, "running commands");

            LabOutputDialog output = new LabOutputDialog(host, "ui-test/command-0");
            output.show("Command output");
            assertTopBackDismisses(ShadowDialog.getLatestDialog(), parent, "command output");

            LabModels.Click click = new LabModels.Click();
            click.action = "webview";
            click.style = "dialog";
            click.title = "Panel";
            click.value = "https://panel.example/";
            LabActions.performClick(host, command, click, java.util.Collections.emptyMap());
            assertTopBackDismisses(ShadowDialog.getLatestDialog(), parent, "embedded WebView");

            assertTrue("all child dialogs leave the parent command sheet open", parent.isShowing());
            parent.dismiss();
        }
    }

    @Test
    public void productionLabDialogCloseButtonsAreFocusableAndWork() {
        try (var hostController = Robolectric.buildActivity(Host.class).setup()) {
            Host host = hostController.get();
            host.layoutWindow();
            LabModels.Command command = host.item.commands.get(0);
            new LabCommandSheet(host, host.item, command, null).show();
            BottomSheetDialog parent = (BottomSheetDialog) ShadowDialog.getLatestDialog();
            assertTrue(parent != null && parent.isShowing());

            parent.findViewById(R.id.sheetBtnEdit).performClick();
            assertDialogCloseButtonWorks((androidx.appcompat.app.AlertDialog) ShadowDialog.getLatestDialog(),
                    DialogInterface.BUTTON_NEGATIVE, parent, "command edit cancel");

            LabModels.Variable variable = new LabModels.Variable();
            variable.key = "sample";
            variable.name = "Sample";
            variable.type = "text";
            LabVariableDialog.show(host, "Parameters", java.util.Collections.singletonList(variable), values -> {});
            assertDialogCloseButtonWorks((androidx.appcompat.app.AlertDialog) ShadowDialog.getLatestDialog(),
                    DialogInterface.BUTTON_NEGATIVE, parent, "variable cancel");

            new LabFileBrowserDialog(host, true, path -> {}).show();
            assertDialogCloseButtonWorks((androidx.appcompat.app.AlertDialog) ShadowDialog.getLatestDialog(),
                    DialogInterface.BUTTON_NEGATIVE, parent, "file browser cancel");

            LabRunningDialog.show(host);
            assertDialogCloseButtonWorks((androidx.appcompat.app.AlertDialog) ShadowDialog.getLatestDialog(),
                    DialogInterface.BUTTON_NEGATIVE, parent, "running list close");

            new LabOutputDialog(host, "ui-test/command-0").show("Command output");
            assertDialogCloseButtonWorks((androidx.appcompat.app.AlertDialog) ShadowDialog.getLatestDialog(),
                    DialogInterface.BUTTON_POSITIVE, parent, "output close");

            LabModels.Click click = new LabModels.Click();
            click.action = "webview";
            click.style = "dialog";
            click.title = "Panel";
            click.value = "https://panel.example/";
            LabActions.performClick(host, command, click, java.util.Collections.emptyMap());
            assertDialogCloseButtonWorks((androidx.appcompat.app.AlertDialog) ShadowDialog.getLatestDialog(),
                    DialogInterface.BUTTON_NEGATIVE, parent, "embedded WebView close");

            assertTrue("closing every child by DPAD must retain the parent sheet", parent.isShowing());
            parent.dismiss();
        }
    }

    @Test
    public void webViewUsesTheAvailableSecondaryDialogViewport() {
        try (var hostController = Robolectric.buildActivity(Host.class).setup()) {
            Host host = hostController.get();
            LabModels.Click click = new LabModels.Click();
            click.action = "webview";
            click.style = "dialog";
            click.title = "Panel 1";
            click.value = "https://panel.example/";
            LabModels.Command command = itemWithCommands(1).commands.get(0);
            LabActions.performClick(host, command, click, java.util.Collections.emptyMap());

            Dialog dialog = ShadowDialog.getLatestDialog();
            assertTrue("the production WebView action must open its AlertDialog", dialog != null && dialog.isShowing());
            int width = dialog.getWindow().getAttributes().width;
            int dialogHeight = dialog.getWindow().getAttributes().height;
            View decor = dialog.getWindow().getDecorView();
            for (int pass = 0; pass < 3; pass++) {
                forceLayoutTree(decor);
                decor.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(dialogHeight, View.MeasureSpec.EXACTLY));
                decor.layout(0, 0, decor.getMeasuredWidth(), decor.getMeasuredHeight());
            }
            WebView actualWebView = dialog.findViewById(R.id.webView);
            assertTrue("the WebView must be attached to the production dialog", actualWebView != null);
            assertEquals("the embedded page must load the click URL", click.value, actualWebView.getUrl());
            View topPanel = dialog.findViewById(androidx.appcompat.R.id.topPanel);
            View buttonPanel = dialog.findViewById(androidx.appcompat.R.id.buttonPanel);
            View parentPanel = dialog.findViewById(androidx.appcompat.R.id.parentPanel);
            View customPanel = dialog.findViewById(androidx.appcompat.R.id.customPanel);
            View contentPanel = dialog.findViewById(androidx.appcompat.R.id.contentPanel);
            View webContainer = dialog.findViewById(R.id.webViewContainer);
            View rootView = webContainer == null ? null : (View) webContainer.getParent();
            assertTrue("the production Dialog must measure a visible WebView viewport; attached=" + actualWebView.isAttachedToWindow()
                            + " bounds="
                            + actualWebView.getLeft() + "," + actualWebView.getTop() + ","
                            + actualWebView.getRight() + "," + actualWebView.getBottom()
                            + " measured=" + actualWebView.getMeasuredWidth() + "x" + actualWebView.getMeasuredHeight()
                            + " visibility=" + actualWebView.getVisibility()
                            + " alpha=" + actualWebView.getAlpha()
                            + " decor=" + decor.getMeasuredWidth() + "x" + decor.getMeasuredHeight()
                            + " parent=" + viewSize(parentPanel) + " top=" + viewSize(topPanel)
                            + " buttons=" + viewSize(buttonPanel) + " content=" + viewSize(contentPanel)
                            + " custom=" + viewSize(customPanel) + " root=" + viewSize(rootView)
                            + " customLp=" + viewLayoutParams(customPanel) + " panelPad=" + (customPanel == null ? "null" : customPanel.getPaddingTop() + "/" + customPanel.getPaddingBottom())
                            + " parentLp=" + viewLayoutParams(parentPanel)
                            + " rootLp=" + (rootView == null ? "null" : rootView.getLayoutParams().width + "x" + rootView.getLayoutParams().height)
                            + " decorPad=" + decor.getPaddingTop() + "/" + decor.getPaddingBottom(),
                    actualWebView.getMeasuredWidth() > 0 && actualWebView.getMeasuredHeight() > 0
                            && actualWebView.getVisibility() == View.VISIBLE && actualWebView.getAlpha() > 0
                            && customPanel != null && customPanel.getMeasuredHeight() > actualWebView.getMeasuredHeight());
            assertEquals("omitted panel height uses the larger landscape WebView default",
                    Math.round(host.getResources().getDisplayMetrics().heightPixels * 0.92f), dialogHeight);
            ((androidx.appcompat.app.AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).performClick();
            ShadowLooper.idleMainLooper();
            Intent browserIntent = host.startedIntent;
            assertTrue("browser action must open the exact URL passed to the embedded WebView",
                    browserIntent != null && click.value.equals(browserIntent.getDataString()));
        }
    }

    private static void sendDpad(View target, int keyCode) {
        long downTime = android.os.SystemClock.uptimeMillis();
        target.dispatchKeyEvent(dpadEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode));
        target.dispatchKeyEvent(dpadEvent(downTime, downTime + 1, KeyEvent.ACTION_UP, keyCode));
    }

    private static void moveFocusByDpad(View root, View from, int direction) {
        int keyCode = direction == View.FOCUS_UP ? KeyEvent.KEYCODE_DPAD_UP
                : direction == View.FOCUS_DOWN ? KeyEvent.KEYCODE_DPAD_DOWN
                : direction == View.FOCUS_LEFT ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
        long downTime = android.os.SystemClock.uptimeMillis();
        boolean handled = from.dispatchKeyEvent(dpadEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode));
        from.dispatchKeyEvent(dpadEvent(downTime, downTime + 1, KeyEvent.ACTION_UP, keyCode));
        if (root.findFocus() != from || handled) return;
        View next = FocusFinder.getInstance().findNextFocus((ViewGroup) root, from, direction);
        if (next != null) next.requestFocus();
    }

    private static void assertSameFocus(View root, View expected, String message) {
        assertEquals(message, expected, root.findFocus());
    }

    private static View layoutDialog(Dialog dialog, Host host) {
        View decor = dialog.getWindow().getDecorView();
        int width = host.getResources().getDisplayMetrics().widthPixels;
        int height = host.getResources().getDisplayMetrics().heightPixels;
        for (int pass = 0; pass < 3; pass++) {
            forceLayoutTree(decor);
            decor.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            decor.layout(0, 0, decor.getMeasuredWidth(), decor.getMeasuredHeight());
        }
        return decor;
    }

    private static void pressBack(Dialog dialog) {
        dialog.onBackPressed();
    }

    private static void assertTopBackDismisses(Dialog child, Dialog parent, String name) {
        assertTrue(name + " must be a real showing Dialog", child != null && child.isShowing() && child != parent);
        pressBack(child);
        assertFalse("BACK closes only the topmost " + name + " dialog", child.isShowing());
        assertTrue("closing " + name + " leaves its parent sheet open", parent.isShowing());
    }

    private static void assertDialogCloseButtonWorks(androidx.appcompat.app.AlertDialog child,
                                                       int buttonId, Dialog parent, String name) {
        assertTrue(name + " must be a real showing AlertDialog", child != null && child.isShowing());
        View button = child.getButton(buttonId);
        assertTrue(name + " must expose an enabled, focusable close control",
                button != null && button.isEnabled() && button.isFocusable() && button.isClickable());
        button.setFocusableInTouchMode(true);
        assertTrue(name + " close control must accept DPAD focus", button.requestFocus());
        assertTrue(name + " close control must own focus before DPAD confirm", button.isFocused());
        button.performClick();
        ShadowLooper.idleMainLooper();
        assertFalse("the focused close control closes " + name, child.isShowing());
        assertTrue("closing " + name + " leaves its parent sheet open", parent.isShowing());
    }

    private static KeyEvent dpadEvent(long downTime, long eventTime, int action, int keyCode) {
        return new KeyEvent(downTime, eventTime, action, keyCode, 0, 0, -1, 0,
                KeyEvent.FLAG_FROM_SYSTEM, android.view.InputDevice.SOURCE_DPAD);
    }

    private static LabModels.Item itemWithCommands(int count) {
        LabModels.Item item = new LabModels.Item();
        item.name = "ui-test";
        item.commands = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            LabModels.Command command = new LabModels.Command();
            command.id = "command-" + i;
            command.name = "Command " + i;
            command.command = "true";
            item.commands.add(command);
        }
        return item;
    }

    private static LabModels.Variable variable(String key, String name, String type, String value) {
        LabModels.Variable variable = new LabModels.Variable();
        variable.key = key;
        variable.name = name;
        variable.type = type;
        variable.defaultValue = value;
        variable.required = true;
        return variable;
    }

    private static int dp(Host host, int value) {
        return Math.round(value * host.getResources().getDisplayMetrics().density);
    }

    private static void forceLayoutTree(View view) {
        view.forceLayout();
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) forceLayoutTree(group.getChildAt(i));
    }

    private static String viewSize(View view) {
        return view == null ? "null" : view.getMeasuredWidth() + "x" + view.getMeasuredHeight()
                + "/" + view.getVisibility();
    }

    private static String viewLayoutParams(View view) {
        return view == null || view.getLayoutParams() == null ? "null"
                : view.getLayoutParams().width + "x" + view.getLayoutParams().height;
    }
}
