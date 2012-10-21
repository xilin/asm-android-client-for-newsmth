package com.athena.asm.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.athena.asm.ActivityFragmentTargets;
import com.athena.asm.HomeActivity;
import com.athena.asm.OnOpenActivityFragmentListener;
import com.athena.asm.PostListActivity;
import com.athena.asm.ProgressDialogProvider;
import com.athena.asm.R;
import com.athena.asm.aSMApplication;
import com.athena.asm.Adapter.BoardTypeListAdapter;
import com.athena.asm.Adapter.SubjectListAdapter;
import com.athena.asm.data.Board;
import com.athena.asm.data.Subject;
import com.athena.asm.util.SmthSupport;
import com.athena.asm.util.StringUtility;
import com.athena.asm.util.task.LoadSubjectTask;
import com.athena.asm.view.PaginationNavigationView;
import com.athena.asm.view.PaginationNavigationView.NavigationAction;
import com.athena.asm.viewmodel.BaseViewModel;
import com.athena.asm.viewmodel.SubjectListViewModel;
import com.markupartist.android.widget.PullToRefreshListView;
import com.markupartist.android.widget.PullToRefreshListView.OnRefreshListener;

public class SubjectListFragment extends SherlockFragment implements
		android.content.DialogInterface.OnClickListener,
		BaseViewModel.OnViewModelChangObserver, 
		PaginationNavigationView.OnPaginationNavigationActionListener {

	private LayoutInflater m_inflater;

	private SubjectListViewModel m_viewModel;

	private PaginationNavigationView m_pageNavigationView;

	private boolean m_isNewInstance = false;
	
	private ProgressDialogProvider m_progressDialogProvider;
	private OnOpenActivityFragmentListener m_onOpenActivityFragmentListener;
	
	public static int BOARD_TYPE_SUBJECT = 0;
	public static int BOARD_TYPE_NORMAL = 1;
	public static int BOARD_TYPE_DIGEST = 2;
	public static int BOARD_TYPE_MARK = 3;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		setRetainInstance(true);
		m_isNewInstance = true;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		m_inflater = inflater;
		View subjectListView = inflater.inflate(R.layout.subject_list, null);

		aSMApplication application = (aSMApplication) getActivity()
				.getApplication();
		m_viewModel = application.getSubjectListViewModel();
		m_viewModel.registerViewModelChangeObserver(this);

		m_pageNavigationView = (PaginationNavigationView)subjectListView.findViewById(R.id.pagination_nav);
		m_pageNavigationView.setNavigationActionListener(this);
		m_pageNavigationView.setPageNumberText(m_viewModel.getCurrentPageNumber() + "");
		
		return subjectListView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		Activity parentActivity = getSherlockActivity();
		if (parentActivity instanceof ProgressDialogProvider) {
			m_progressDialogProvider = (ProgressDialogProvider) parentActivity;
		}
		if (parentActivity instanceof OnOpenActivityFragmentListener) {
			m_onOpenActivityFragmentListener = (OnOpenActivityFragmentListener) parentActivity;
		}

		boolean isNewBoard = false;
		if (m_isNewInstance) {
			String defaultBoardType = ((aSMApplication) getActivity()
					.getApplication()).getDefaultBoardType();
			Board currentBoard = (Board) getActivity().getIntent()
					.getSerializableExtra(StringUtility.BOARD);
			isNewBoard = m_viewModel.updateCurrentBoard(currentBoard,
					defaultBoardType);
			m_viewModel.setIsFirstIn(isNewBoard);
		}
		m_isNewInstance = false;

		if (isNewBoard) {
			refreshSubjectList();
		} else {
			reloadSubjectList();
		}
	}

	@Override
	public void onDestroy() {
		m_viewModel.unregisterViewModelChangeObserver(this);
		super.onDestroy();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
		case Activity.RESULT_OK:
			Bundle b = data.getExtras();
			boolean isToRefreshBoard = b
					.getBoolean(StringUtility.REFRESH_BOARD);
			if (isToRefreshBoard) {
				refreshSubjectList();
			}
			break;

		default:
			break;
		}
	}

	public void reloadSubjectList() {
		if (m_viewModel.getSubjectList() != null) {
			if (m_viewModel.isFirstIn()) {
				m_viewModel.gotoFirstPage();
				m_pageNavigationView.setPageNumberText(m_viewModel.getCurrentPageNumber() + "");
				m_viewModel.setIsFirstIn(false);
			}

			PullToRefreshListView listView = (PullToRefreshListView) getActivity()
					.findViewById(R.id.subject_list);
			listView.onRefreshComplete();
			listView.setAdapter(new SubjectListAdapter(m_inflater, m_viewModel
					.getSubjectList()));

			listView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						final int position, long id) {
					if (m_onOpenActivityFragmentListener != null) {
						Bundle bundle = new Bundle();
						bundle.putSerializable(StringUtility.SUBJECT, (Subject)view.getTag());
						bundle.putInt(StringUtility.BOARD_TYPE, m_viewModel.getBoardType());
						m_onOpenActivityFragmentListener.onOpenActivityOrFragment(ActivityFragmentTargets.POST_LIST, bundle);
					}
				}
			});

			listView.setOnRefreshListener(new OnRefreshListener() {

				@Override
				public void onRefresh() {
					refreshSubjectList();
				}
			});

			getActivity().setTitle(m_viewModel.getTitleText());

			listView.requestFocus();
		}
	}

	private void refreshSubjectList() {
		LoadSubjectTask loadSubjectTask = new LoadSubjectTask(m_viewModel);
		loadSubjectTask.execute();
		if (m_progressDialogProvider != null) {
			m_progressDialogProvider.showProgressDialog();
		}
	}

	public static final int SWITCH_BOARD_TYPE = Menu.FIRST;
	public static final int REFRESH_SUBJECTLIST = Menu.FIRST + 1;
	public static final int SEARCH_POST = Menu.FIRST + 2;
	public static final int CREATE_ID = Menu.FIRST + 3;
	public static final int QUICK_SWITCH_BOARD_TYPE = Menu.FIRST + 4;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// super.onCreateOptionsMenu(menu, inflater);
		boolean isLight = !aSMApplication.getCurrentApplication().isNightTheme();

		if (SmthSupport.getInstance().getLoginStatus()) {
			menu.add(0, CREATE_ID, Menu.NONE, "发新贴")
					.setIcon(
							isLight ? R.drawable.write_inverse
									: R.drawable.write)
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}
		menu.add(0, QUICK_SWITCH_BOARD_TYPE, Menu.NONE, "模式切换")
				.setIcon(
						isLight ? R.drawable.switcher_inverse
								: R.drawable.switcher)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, REFRESH_SUBJECTLIST, Menu.NONE, "刷新")
				.setIcon(
						isLight ? R.drawable.refresh_inverse
								: R.drawable.refresh)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, SEARCH_POST, Menu.NONE, "搜索")
				.setIcon(
						isLight ? R.drawable.search_inverse : R.drawable.search)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, SWITCH_BOARD_TYPE, Menu.NONE, "切换到...").setShowAsAction(
				MenuItem.SHOW_AS_ACTION_NEVER
						| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent homeIntent = new Intent(getActivity(), HomeActivity.class);
			homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(homeIntent);
			break;
		case QUICK_SWITCH_BOARD_TYPE:
			m_viewModel.toggleBoardType();
			m_viewModel.setIsFirstIn(true);
			refreshSubjectList();
			break;
		case SWITCH_BOARD_TYPE:
			// String[] items = { "同主题", "普通模式", "文摘区", "保留区" };
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.post_alert_title);
			// builder.setItems(items,this);
			builder.setAdapter(
					new BoardTypeListAdapter(m_viewModel.getBoardType(),
							m_inflater), this);
			AlertDialog alert = builder.create();
			alert.show();
			break;
		case REFRESH_SUBJECTLIST:
			refreshSubjectList();
			break;
		case SEARCH_POST:
			Intent postIntent = new Intent();
			postIntent.setClassName("com.athena.asm",
					"com.athena.asm.SearchPostActivity");
			postIntent.putExtra(StringUtility.BOARD, m_viewModel
					.getCurrentBoard().getEngName());
			postIntent.putExtra(StringUtility.BID, m_viewModel
					.getCurrentBoard().getBoardID());
			startActivity(postIntent);
			break;
		case CREATE_ID:
			Intent writeIntent = new Intent();
			writeIntent.setClassName("com.athena.asm",
					"com.athena.asm.WritePostActivity");
			writeIntent.putExtra(StringUtility.URL,
					"http://www.newsmth.net/bbspst.php?board="
							+ m_viewModel.getCurrentBoard().getEngName());
			writeIntent.putExtra(StringUtility.WRITE_TYPE, 0);
			writeIntent.putExtra(StringUtility.IS_REPLY, false);
			// startActivity(intent);
			startActivityForResult(writeIntent, 0);
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		m_viewModel.setIsFirstIn(true);
		m_viewModel.setBoardType(which);
		LoadSubjectTask loadSubjectTask = new LoadSubjectTask(m_viewModel);
		loadSubjectTask.execute();
		dialog.dismiss();
		if (m_progressDialogProvider != null) {
			m_progressDialogProvider.showProgressDialog();
		}
	}

	@Override
	public void onViewModelChange(BaseViewModel viewModel,
			String changedPropertyName, Object... params) {
		if (changedPropertyName
				.equals(SubjectListViewModel.SUBJECTLIST_PROPERTY_NAME)) {
			reloadSubjectList();
			if (m_progressDialogProvider != null) {
				m_progressDialogProvider.dismissProgressDialog();
			}
		}
	}

	@Override
	public void onNavigationAction(NavigationAction navAction) {
		if (navAction == NavigationAction.GoFirst) {
			m_viewModel.gotoFirstPage();
		} else if (navAction == NavigationAction.GoLast) {
			m_viewModel.gotoLastPage();
		} else if (navAction == NavigationAction.GoPrev) {
			m_viewModel.gotoPrevPage();
		} else if (navAction == NavigationAction.GoPageNumber) {
			int pageSet = m_pageNavigationView.getCurrentPageNumber();
			m_viewModel.setCurrentPageNumber(pageSet);
			
		} else if (navAction == NavigationAction.GoNext) {
			m_viewModel.gotoNextPage();
		}

		m_viewModel.updateBoardCurrentPage();
		m_pageNavigationView.setPageNumberText(m_viewModel.getCurrentPageNumber() + "");
		refreshSubjectList();		
	}

}
