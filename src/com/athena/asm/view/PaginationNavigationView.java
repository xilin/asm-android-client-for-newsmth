package com.athena.asm.view;

import com.athena.asm.R;
import com.athena.asm.aSMApplication;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;;


public class PaginationNavigationView extends LinearLayout 
									  implements OnClickListener,
									  OnTouchListener {
	
	public enum NavigationAction {
		GoFirst,
		GoPrev,
		GoNext,
		GoLast,
		GoPageNumber
	}
	
	public interface OnPaginationNavigationActionListener {
		public void onNavigationAction(NavigationAction navAction);
	}

	private EditText m_pageNoEditText;
	private Button m_firstButton;
	private Button m_lastButton;
	private Button m_preButton;
	private Button m_goButton;
	private Button m_nextButton;
	
	private boolean m_isGoCanBeUsedAsLast = false;
	private boolean m_isPageNoEditTextTouched = false;
	
	private OnPaginationNavigationActionListener m_navActionListener;
	
	public PaginationNavigationView(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.pagination_nav_view, this);
		
		initView();
	}
	
	private void initView() {
		m_pageNoEditText = (EditText) findViewById(R.id.edittext_page_no);
		m_pageNoEditText.setOnClickListener(this);
		m_pageNoEditText.setOnTouchListener(this);
		
		m_firstButton = (Button)findViewById(R.id.btn_first_page);
		m_firstButton.setOnClickListener(this);
		m_lastButton = (Button)findViewById(R.id.btn_last_page);
		m_lastButton.setOnClickListener(this);
		m_lastButton.setVisibility(View.GONE);
		m_preButton = (Button)findViewById(R.id.btn_pre_page);
		m_preButton.setOnClickListener(this);
		m_goButton = (Button)findViewById(R.id.btn_go_page);
		m_goButton.setOnClickListener(this);
		m_goButton.setText(R.string.go_page);
		m_nextButton = (Button)findViewById(R.id.btn_next_page);
		m_nextButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.edittext_page_no) {
			changePageNoEditStatus();
			return;
		}
		
		NavigationAction action = NavigationAction.GoNext;
		if (view.getId() == R.id.btn_first_page) {
			action = NavigationAction.GoFirst;
		}
		else if (view.getId() == R.id.btn_last_page) {
			action = NavigationAction.GoLast;
		}
		else if (view.getId() == R.id.btn_pre_page) {
			action = NavigationAction.GoPrev;
		}
		else if (view.getId() == R.id.btn_next_page) {
			action = NavigationAction.GoNext;
		}
		else if (view.getId() == R.id.btn_go_page) {
			// 如果未按过编辑框，GO的功能为末页。否则为GO
			if (m_isGoCanBeUsedAsLast && !m_isPageNoEditTextTouched) {
				action = NavigationAction.GoLast;
			}
			else {
				action = NavigationAction.GoPageNumber;
			}
		}
		
		if (m_navActionListener != null) {
			m_navActionListener.onNavigationAction(action);
		}
	}
	
	public void setNavigationActionListener(OnPaginationNavigationActionListener listener) {
		m_navActionListener = listener;
	}
	
	public void disableActions() {
		m_firstButton.setEnabled(false);
		m_preButton.setEnabled(false);
		m_nextButton.setEnabled(false);
		m_lastButton.setEnabled(false);
	}
	
	public int getCurrentPageNumber() {
		return Integer.parseInt(m_pageNoEditText.getText().toString());
	}
	
	public void setPageNumberText(String pageNumberText) {
		m_pageNoEditText.setText(pageNumberText);
	}
	
	public void setInvisibleMode() {
		m_goButton.setVisibility(View.GONE);
		m_pageNoEditText.setVisibility(View.GONE);
		m_firstButton.setVisibility(View.GONE);
		m_lastButton.setVisibility(View.GONE);
		m_preButton.setVisibility(View.GONE);
		m_nextButton.setVisibility(View.GONE);
	}
	
	public void setNormalPostMode() {
		m_goButton.setVisibility(View.GONE);
		m_pageNoEditText.setVisibility(View.GONE);
		m_lastButton.setVisibility(View.VISIBLE);
		m_firstButton.setText(R.string.topic_first_page);
		m_lastButton.setText(R.string.topic_all_page);
		m_preButton.setText(R.string.topic_pre_page);
		m_nextButton.setText(R.string.topic_next_page);
	}
	
	public void setSubjectPostMode() {
		m_goButton.setVisibility(View.VISIBLE);
		m_pageNoEditText.setVisibility(View.VISIBLE);
		m_lastButton.setVisibility(View.GONE);
		m_firstButton.setText(R.string.first_page);
		m_lastButton.setText(R.string.last_page);
		m_preButton.setText(R.string.pre_page);
		m_nextButton.setText(R.string.next_page);
	}
	
	public void setGoCanBeUsedAsLast() {
		m_isGoCanBeUsedAsLast = true;
		m_pageNoEditText.setTextColor(Color.GRAY);
		m_goButton.setText(R.string.go_and_last_page);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (m_isGoCanBeUsedAsLast) {
			changePageNoEditStatus();
		}
		
		return false;
	}
	
	private void changePageNoEditStatus() {
		if (aSMApplication.getCurrentApplication().isNightTheme()) {
			m_pageNoEditText.setTextColor(Color.WHITE);
		} else {
			m_pageNoEditText.setTextColor(Color.BLACK);
		}
		
		m_isPageNoEditTextTouched = true;
	}
	
	
	
}
