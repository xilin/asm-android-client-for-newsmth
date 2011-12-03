package com.athena.asm.Adapter;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.athena.asm.MailListActivity;
import com.athena.asm.R;
import com.athena.asm.aSMApplication;
import com.athena.asm.data.Mail;
import com.athena.asm.util.StringUtility;

public class MailListAdapter extends BaseAdapter {

	private MailListActivity activity;
	private LayoutInflater inflater;
	private List<Mail> mailList;

	public MailListAdapter(MailListActivity activity, LayoutInflater inflater, List<Mail> mailList) {
		this.activity = activity;
		this.inflater = inflater;
		this.mailList = mailList;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View layout = null;
		if (convertView != null) {
			layout = convertView;
		}
		else {
			layout = inflater.inflate(R.layout.mail_list_item, null);
		}
		
		aSMApplication application = (aSMApplication)activity.getApplication();
		
		Mail mail = mailList.get(mailList.size() - position - 1);
		
		TextView authorTextView = (TextView) layout.findViewById(R.id.SenderID);
		if (mail.getStatus().length() > 0) {
			authorTextView.setText("【"+ mail.getStatus() + "】" + mail.getSenderID());
		}
		else {
			authorTextView.setText(mail.getSenderID());
		}
		TextView titleTextView = (TextView) layout.findViewById(R.id.MailTitle);
		String titleString = mail.getTitle();
		if (mail.isUnread()) {
			TextPaint tp = titleTextView.getPaint();
			tp.setFakeBoldText(true);
		}
		else {
			TextPaint tp = titleTextView.getPaint();
			tp.setFakeBoldText(false);
		}
		titleTextView.setText(Html.fromHtml(titleString));
		titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, application.getSubjectFontSize());
		
		TextView dateTextView = (TextView) layout.findViewById(R.id.MailSendDate);
		dateTextView.setText(mail.getDateString());
		
		layout.setTag(mail);
		layout.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				Bundle bundle = new Bundle();
				bundle.putSerializable(StringUtility.MAIL, (Mail)v.getTag());
				intent.putExtras(bundle);
				intent.setClassName("com.athena.asm", "com.athena.asm.ReadMailActivity");
				activity.startActivity(intent);
			}
		});

		return layout;
	}

	@Override
	public int getCount() {
		return mailList.size();
	}

	@Override
	public Object getItem(int position) {
		return mailList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
}