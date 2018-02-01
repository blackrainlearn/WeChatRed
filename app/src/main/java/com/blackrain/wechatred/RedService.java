package com.blackrain.wechatred;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class RedService extends AccessibilityService
{
    private static final String TAG = "RedService";
    public RedService()
    {
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event)
    {
        int eventType = event.getEventType();
        switch (eventType)
        {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                List<CharSequence> textList = event.getText();
                if(textList.isEmpty())
                    return;
                for (CharSequence text :textList)
                {
                    String content = text.toString();
                    if (content.contains("[微信红包]"))
                    {
                        if (event.getParcelableData() != null && (event.getParcelableData() instanceof Notification))
                        {
                            Notification notification = (Notification)event.getParcelableData();
                            PendingIntent pendingIntent = notification.contentIntent;
                            try
                            {
                                pendingIntent.send();
                            }
                            catch (Exception ex)
                            {
                                Log.d("RedService", ex.toString());
                            }
                        }

                    }
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                String className = event.getClassName().toString();
                Log.d(TAG, "WINDOWS_CHANGED: " + className);
                if (className.equals("com.tencent.mm.ui.LauncherUI"))
                {
                    Log.d(TAG, "开始分析红包并点击最后一个红包");
                    getLastPacket();
                }
                else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI"))
                {
                    Log.d(TAG, "开红包");
                    inputClick("com.tencent.mm:id/c2i");
                }
                else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI")) {
                    Log.d(TAG, "退出红包");
                    inputClick("com.tencent.mm:id/ho");
                }
                break;
        }
    }

    /**
     * 根据id,获取AccessibilityNodeInfo，并点击。
     */
    private void inputClick(String clickId) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null)
        {
            List<AccessibilityNodeInfo> list = nodeInfo
                    .findAccessibilityNodeInfosByViewId(clickId);
            for (AccessibilityNodeInfo item : list)
            {
                item.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    private void getLastPacket()
    {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        parents.clear();
        recycle(nodeInfo);
        Log.d(TAG, "红包个数：" + parents.size());
        if (parents.size() > 0)
        {
            parents.get(parents.size() - 1).performAction(
                    AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    ArrayList<AccessibilityNodeInfo> parents = new ArrayList<AccessibilityNodeInfo>();
    /**
     * 这个方法是用递归的方式，遍历节点树。
     * 如果找到“领取红包”和“查看红包”所在叶子节点，就用while不断的找自己父节点，这个父节点要求可以被点击。（也是是说找最近一个可以点击的父节点）
     */
    private void recycle(AccessibilityNodeInfo info)
    {
        if (info.getChildCount() == 0)
        {
            if (info.getText() != null)
            {
                if ("领取红包".equals(info.getText().toString()))
                {
//					if (info.isClickable()) {
//						info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//					}
                    Log.d(TAG, "=========>有红包啦");
                    AccessibilityNodeInfo parent = info.getParent();
                    while (parent != null)
                    {
                        if (parent.isClickable())
                        {
                            parents.add(parent);//找到了添加到列表并推出循环，否则继续往上找父节点。
                            break;
                        }
                        parent = parent.getParent();
                    }
                }
            }
        }
        else
        {
            for (int i = 0; i < info.getChildCount(); i++)
            {
                if (info.getChild(i) != null)
                {
                    recycle(info.getChild(i));
                }
            }
        }
    }

    @Override
    public void onInterrupt()
    {

    }

    @Override
    protected void onServiceConnected()
    {
        super.onServiceConnected();
        Toast.makeText(RedService.this, "成功与微信绑定，开始监听", Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        Toast.makeText(RedService.this, "与微信解除绑定", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }
}
