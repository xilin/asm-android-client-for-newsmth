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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.view.KeyEvent;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;


public class PaginationNavigationView extends LinearLayout 
									  implements OnClickListener, OnLongClickListener,
									  OnTouchListener, OnKeyListener {
	
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
	private ImageButton m_preButton;
	private ImageButton m_nextButton;
	
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
		m_pageNoEditText.setOnKeyListener(this);
		
		m_preButton = (ImageButton)findViewById(R.id.btn_pre_page);
		m_preButton.setOnClickListener(this);
		m_preButton.setLongClickable(true);
		m_preButton.setOnLongClickListener(this);
		
		m_nextButton = (ImageButton)findViewById(R.id.btn_next_page);
		m_nextButton.setOnClickListener(this);
		m_nextButton.setLongClickable(true);
		m_nextButton.setOnLongClickListener(this);
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
	
	@Override
	public boolean onLongClick(View view) {
		if (view.getId() == R.id.btn_pre_page) {
			NavigationAction action = NavigationAction.GoFirst;
			if (m_navActionListener != null) {
				m_navActionListener.onNavigationAction(action);
			}
		}
		else if (view.getId() == R.id.btn_next_page) {
			NavigationAction action = NavigationAction.GoLast;
			if (m_navActionListener != null) {
				m_navActionListener.onNavigationAction(action);
			}
		}
		
		return true;
	}
	
	public void setNavigationActionListener(OnPaginationNavigationActionListener listener) {
		m_navActionListener = listener;
	}
	
	public void disableActions() {
		m_preButton.setEnabled(false);
		m_nextButton.setEnabled(false);
	}
	
	public int getCurrentPageNumber() {
		return Integer.parseInt(m_pageNoEditText.getText().toString());
	}
	
	public void setPageNumberText(String pageNumberText) {
		m_pageNoEditText.setText(pageNumberText);
	}
	
	public void setInvisibleMode() {
		m_pageNoEditText.setVisibility(View.GONE);
		m_preButton.setVisibility(View.GONE);
		m_nextButton.setVisibility(View.GONE);
	}
	
	public void setNormalPostMode() {
		m_pageNoEditText.setVisibility(View.GONE);
	}
	
	public void setSubjectPostMode() {
		m_pageNoEditText.setVisibility(View.VISIBLE);
	}
	
	public void setGoCanBeUsedAsLast() {
		//m_isGoCanBeUsedAsLast = true;
		//m_pageNoEditText.setTextColor(Color.GRAY);
		//m_goButton.setText(R.string.go_and_last_page);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (m_isGoCanBeUsedAsLast) {
			changePageNoEditStatus();
		}
		
		return false;
	}
	
	private void changePageNoEditStatus() {
		m_isPageNoEditTextTouched = true;
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_ENTER) {
			if (m_navActionListener != null) {
				m_navActionListener.onNavigationAction(NavigationAction.GoPageNumber);
				
				InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	            imm.hideSoftInputFromWindow(getWindowToken(), 0);
	       
			}
			return true;
		}
		
		return false;
	}

	
	
	
	
}
