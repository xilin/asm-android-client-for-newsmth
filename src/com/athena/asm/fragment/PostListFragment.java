package com.athena.asm.fragment;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.Editable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.athena.asm.ActivityFragmentTargets;
import com.athena.asm.OnOpenActivityFragmentListener;
import com.athena.asm.ProgressDialogProvider;
import com.athena.asm.R;
import com.athena.asm.WritePostActivity;
import com.athena.asm.aSMApplication;
import com.athena.asm.Adapter.PostListAdapter;
import com.athena.asm.data.Board;
import com.athena.asm.data.Mail;
import com.athena.asm.data.Post;
import com.athena.asm.data.Subject;
import com.athena.asm.util.StringUtility;
import com.athena.asm.util.task.ForwardPostToMailTask;
import com.athena.asm.util.task.LoadPostTask;
import com.athena.asm.view.PaginationNavigationView;
import com.athena.asm.view.PaginationNavigationView.NavigationAction;
import com.athena.asm.viewmodel.BaseViewModel;
import com.athena.asm.viewmodel.PostListViewModel;

public class PostListFragment extends SherlockFragment implements
		OnTouchListener, OnLongClickListener,
		OnGestureListener, BaseViewModel.OnViewModelChangObserver,
		PaginationNavigationView.OnPaginationNavigationActionListener {

	private LayoutInflater m_inflater;

	private PostListViewModel m_viewModel;

	private PaginationNavigationView m_pageNavigationView;

	private int m_screenHeight;
	private ListView m_listView;

	private GestureDetector m_GestureDetector;

	private boolean m_isNewInstance = false;
	private boolean m_isFromReplyOrAt = false;

	private boolean m_isNewTouchStart = false;
	private float m_touchStartX = 0;
	private float m_touchStartY = 0;

	private float m_touchCurrentX = 0;
	private float m_touchCurrentY = 0;

	private int m_startNumber = 0;

	private String m_url = null;

	private ShareActionProvider actionProvider;
	
	private ProgressDialogProvider m_progressDialogProvider;
	private OnOpenActivityFragmentListener m_onOpenActivityFragmentListener;

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
		View postListView = inflater.inflate(R.layout.post_list, null);

		aSMApplication application = (aSMApplication) getActivity()
				.getApplication();
		m_viewModel = application.getPostListViewModel();
		m_viewModel.registerViewModelChangeObserver(this);

		this.m_screenHeight = getActivity().getWindowManager()
				.getDefaultDisplay().getHeight();
		
		m_pageNavigationView = (PaginationNavigationView)postListView.findViewById(R.id.pagination_nav);
		m_pageNavigationView.setNavigationActionListener(this);
		m_pageNavigationView.setGoCanBeUsedAsLast();

		m_listView = (ListView) postListView.findViewById(R.id.post_list);

		m_viewModel.setBoardType(getActivity().getIntent().getIntExtra(
				StringUtility.BOARD_TYPE, 0));
		m_viewModel.setIsToRefreshBoard(false);

		m_GestureDetector = new GestureDetector(this);

		return postListView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		boolean isNewSubject = false;
		
		Activity parentActivity = getSherlockActivity();
		if (parentActivity instanceof ProgressDialogProvider) {
			m_progressDialogProvider = (ProgressDialogProvider) parentActivity;
		}
		if (parentActivity instanceof OnOpenActivityFragmentListener) {
			m_onOpenActivityFragmentListener = (OnOpenActivityFragmentListener) parentActivity;
		}

		if (m_isNewInstance) {
			Subject newSubject = (Subject) getActivity().getIntent()
					.getSerializableExtra(StringUtility.SUBJECT);
			if (newSubject != null) {
				isNewSubject = m_viewModel.updateSubject(newSubject);
			} else {
				m_isFromReplyOrAt = true;
				Mail mail = (Mail) getActivity().getIntent()
						.getSerializableExtra(StringUtility.MAIL);
				if (mail.getBoxType() == 4) {
					m_url = "http://m.newsmth.net/refer/at/read?index="
							+ mail.getNumber();
				} else {
					m_url = "http://m.newsmth.net/refer/reply/read?index="
							+ mail.getNumber();
				}
			}

			m_isNewInstance = false;
		}

		if (isNewSubject) {
			LoadPostTask loadPostTask = new LoadPostTask(m_viewModel,
					m_viewModel.getCurrentSubject(), 0, false, false,
					m_startNumber, null);
			loadPostTask.execute();
			if (m_progressDialogProvider != null) {
				m_progressDialogProvider.showProgressDialog();
			}
		} else if (m_isFromReplyOrAt) {
			LoadPostTask loadPostTask = new LoadPostTask(m_viewModel,
					m_viewModel.getCurrentSubject(), 0, false, false,
					m_startNumber, m_url);
			loadPostTask.execute();
			if (m_progressDialogProvider != null) {
				m_progressDialogProvider.showProgressDialog();
			}
		} else {
			reloadPostList();
		}
	}

	@Override
	public void onDestroy() {
		m_viewModel.unregisterViewModelChangeObserver(this);

		super.onDestroy();
	}

	public void reloadPostList() {
		if (m_viewModel.getPostList() == null) {

			m_viewModel.ensurePostExists();
			
			m_pageNavigationView.disableActions();
		}

		actionProvider.setShareIntent(createShareIntent());

		m_listView.setAdapter(new PostListAdapter(this, m_inflater, m_viewModel
				.getPostList()));

		m_viewModel.updateCurrentPageNumberFromSubject();
		m_pageNavigationView.setPageNumberText(m_viewModel.getCurrentPageNumber() + "");
		m_listView.requestFocus();

		m_viewModel.setIsPreloadFinished(false);
		m_viewModel.updatePreloadSubjectFromCurrentSubject();

		if (m_viewModel.getBoardType() == SubjectListFragment.BOARD_TYPE_SUBJECT) {
			m_pageNavigationView.setSubjectPostMode();
		} else if (m_viewModel.getBoardType() == SubjectListFragment.BOARD_TYPE_NORMAL
				&& !m_isFromReplyOrAt) {
			m_pageNavigationView.setNormalPostMode();
		} else {
			m_pageNavigationView.setInvisibleMode();
		}
		getActivity().setTitle(m_viewModel.getSubjectTitle());

		if (m_viewModel.getBoardType() == 0) {
			int nextPage = m_viewModel.getNextPageNumber();
			if (nextPage > 0) {
				m_viewModel.getPreloadSubject().setCurrentPageNo(nextPage);
				LoadPostTask loadPostTask = new LoadPostTask(m_viewModel,
						m_viewModel.getPreloadSubject(), 0, true, false,
						m_startNumber, null);
				loadPostTask.execute();
			}
		} else {
			LoadPostTask loadPostTask = new LoadPostTask(m_viewModel,
					m_viewModel.getPreloadSubject(), 3, true, false,
					m_startNumber, null);
			loadPostTask.execute();
		}
	}

	@Override
	public void onViewModelChange(BaseViewModel viewModel,
			String changedPropertyName, Object... params) {

		if (changedPropertyName
				.equals(PostListViewModel.POSTLIST_PROPERTY_NAME)) {
			reloadPostList();
			if (m_progressDialogProvider != null) {
				m_progressDialogProvider.dismissProgressDialog();
			}
		}

	}

	@Override
	public boolean onDown(MotionEvent e) {
		m_isNewTouchStart = true;
		m_touchStartX = e.getX();
		m_touchStartY = e.getY();
		m_touchCurrentX = m_touchStartX;
		m_touchCurrentY = m_touchStartY;
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {

	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		m_touchCurrentX += distanceX;
		m_touchCurrentY += distanceY;
		return true;
	}

	@Override
	public void onShowPress(MotionEvent e) {

	}

	private void setListOffset(int jump) {
		int index = m_listView.getFirstVisiblePosition();
		Log.d("move", String.valueOf(index));
		int newIndex = index + jump;
		if (newIndex == -1) {
			newIndex = 0;
		} else if (m_listView.getItemAtPosition(newIndex) == null) {
			newIndex = index;
		}
		m_listView.setSelectionFromTop(newIndex, 0);
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		if (aSMApplication.getCurrentApplication().isTouchScroll()) {
			int touchY = (int) e.getRawY();
			float scale = (float) (m_screenHeight / 800.0);
			if (touchY > 60 * scale && touchY < 390 * scale) {
				setListOffset(-1);
			} else if (touchY > 410 * scale && touchY < 740 * scale) {
				setListOffset(1);
			}
		}
		return false;
	}

	@Override
	public boolean onLongClick(View v) {
		if (m_viewModel.getSmthSupport().getLoginStatus()) {
			RelativeLayout relativeLayout = null;
			if (v.getId() == R.id.PostContent) {
				relativeLayout = (RelativeLayout) v.getParent();
			} else {
				relativeLayout = (RelativeLayout) v;
			}
			final String authorID = (String) ((TextView) relativeLayout
					.findViewById(R.id.AuthorID)).getText();
			final Post post = ((PostListAdapter.ViewHolder) relativeLayout
					.getTag()).post;
			final Post firstPost = m_viewModel.getPostList().get(0);
			List<String> itemList = new ArrayList<String>();
			itemList.add(getString(R.string.post_reply_post));
			itemList.add(getString(R.string.post_reply_mail));
			itemList.add(getString(R.string.post_query_author));
			itemList.add(getString(R.string.post_copy_author));
			itemList.add(getString(R.string.post_copy_content));
			itemList.add(getString(R.string.post_foward_self));
			itemList.add(getString(R.string.post_foward_external));
			itemList.add(getString(R.string.post_group_foward_external));
			if (post.getAuthor().equals(m_viewModel.getSmthSupport().userid)) {
				itemList.add(getString(R.string.post_edit_post));
			}
			final String[] items = new String[itemList.size()];
			itemList.toArray(items);
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.post_alert_title);
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					switch (item) {
					case 0:
						if (m_onOpenActivityFragmentListener != null) {
							Bundle bundle = new Bundle();
							bundle.putSerializable(StringUtility.URL,
												   "http://www.newsmth.net/bbspst.php?board="
												   + post.getBoard() + "&reid="
												   + post.getSubjectID());
							bundle.putSerializable(StringUtility.WRITE_TYPE, WritePostActivity.TYPE_POST);
							bundle.putSerializable(StringUtility.IS_REPLY, true);
							m_onOpenActivityFragmentListener.onOpenActivityOrFragment(ActivityFragmentTargets.WRITE_POST,
									  bundle);
						}
						break;
					case 1:
						if (m_onOpenActivityFragmentListener != null) {
							Bundle bundle = new Bundle();
							bundle.putSerializable(StringUtility.URL,
												   "http://www.newsmth.net/bbspstmail.php?board="
												   + post.getBoard() + "&id="
												   + post.getSubjectID());
							bundle.putSerializable(StringUtility.WRITE_TYPE, WritePostActivity.TYPE_MAIL);
							bundle.putSerializable(StringUtility.IS_REPLY, true);
							m_onOpenActivityFragmentListener.onOpenActivityOrFragment(ActivityFragmentTargets.WRITE_POST,
																					  bundle);
						}
						
						break;
					case 2:
						if (m_onOpenActivityFragmentListener != null) {
							Bundle bundle = new Bundle();
							bundle.putSerializable(StringUtility.USERID, authorID);
							m_onOpenActivityFragmentListener.onOpenActivityOrFragment(ActivityFragmentTargets.VIEW_PROFILE,
																					  bundle);
						}
						break;
					case 3:
						ClipboardManager clip = (ClipboardManager) getActivity()
								.getSystemService(Context.CLIPBOARD_SERVICE);
						clip.setText(authorID);
						Toast.makeText(getActivity(),
								"ID ： " + authorID + "已复制到剪贴板",
								Toast.LENGTH_SHORT).show();
						break;
					case 4:
						ClipboardManager clip2 = (ClipboardManager) getActivity()
								.getSystemService(Context.CLIPBOARD_SERVICE);
						clip2.setText(post.getTextContent());
						Toast.makeText(getActivity(), "帖子内容已复制到剪贴板",
								Toast.LENGTH_SHORT).show();
						break;
					case 5:
						ForwardPostToMailTask task = new ForwardPostToMailTask(
								getActivity(), m_viewModel, post,
								ForwardPostToMailTask.FORWARD_TO_SELF, "");
						task.execute();
						break;
					case 6:
						forwardToEmail(post, false);
						break;
					case 7:
						forwardToEmail(firstPost, true);
						break;
					case 8:
						if (m_onOpenActivityFragmentListener != null) {
							Bundle bundle = new Bundle();
							bundle.putSerializable(StringUtility.URL,
												   "http://www.newsmth.net/bbspst.php?board="
												   + post.getBoard() + "&id="
												   + post.getSubjectID() + "&ftype=");
							bundle.putSerializable(StringUtility.WRITE_TYPE, WritePostActivity.TYPE_POST_EDIT);
							bundle.putSerializable(StringUtility.TITLE, post.getTitle().replace("主题:", ""));
							m_onOpenActivityFragmentListener.onOpenActivityOrFragment(ActivityFragmentTargets.WRITE_POST,
									  												  bundle);
						}
					default:
						break;
					}
					dialog.dismiss();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
		}
		return true;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		boolean isConsumed = m_GestureDetector.onTouchEvent(event);
		if (event.getAction() == MotionEvent.ACTION_CANCEL
				|| event.getAction() == MotionEvent.ACTION_UP) {
			if (m_isNewTouchStart) {
				m_isNewTouchStart = false;
				final int flingMinXDistance = 100, flingMaxYDistance = 100;
				if (m_touchCurrentX - m_touchStartX > flingMinXDistance
						&& Math.abs(m_touchCurrentY - m_touchStartY) < flingMaxYDistance) {
					// Fling left
					Toast.makeText(getActivity(), "下一页", Toast.LENGTH_SHORT)
							.show();
					onNavigationAction(NavigationAction.GoNext);
				} else if (m_touchStartX - m_touchCurrentX > flingMinXDistance
						&& Math.abs(m_touchStartY - m_touchCurrentY) < flingMaxYDistance) {
					// Fling right
					Toast.makeText(getActivity(), "上一页", Toast.LENGTH_SHORT)
							.show();
					onNavigationAction(NavigationAction.GoPrev);
				}
			}

		}
		return isConsumed;
	}

	private void refreshPostList() {
		LoadPostTask loadPostTask = new LoadPostTask(m_viewModel,
				m_viewModel.getCurrentSubject(), 0, false, false,
				m_startNumber, null);
		loadPostTask.execute();
		if (m_progressDialogProvider != null) {
			m_progressDialogProvider.showProgressDialog();
		}
	}

	public static final int REFRESH_SUBJECTLIST = Menu.FIRST;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		boolean isLight = aSMApplication.THEME == R.style.Theme_Sherlock_Light;
		((SherlockFragmentActivity) getActivity()).getSupportMenuInflater()
				.inflate(R.menu.share_action_provider, menu);

		menu.add(0, REFRESH_SUBJECTLIST, Menu.NONE, "刷新")
				.setIcon(
						isLight ? R.drawable.refresh_inverse
								: R.drawable.refresh)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

		MenuItem actionItem = menu
				.findItem(R.id.menu_item_share_action_provider_action_bar);
		actionProvider = (ShareActionProvider) actionItem.getActionProvider();
		actionProvider
				.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// super.onOptionsItemSelected(item);
		if (!m_isFromReplyOrAt) {
			switch (item.getItemId()) {
			case REFRESH_SUBJECTLIST:
				refreshPostList();
				break;
			}
		}

		return true;
	}

	/**
	 * Creates a sharing {@link Intent}.
	 * 
	 * @return The sharing intent.
	 */
	private Intent createShareIntent() {
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		if (m_viewModel.getCurrentSubject() != null) {
			Subject subject = m_viewModel.getCurrentSubject();
			shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject.getTitle());
			if (m_isFromReplyOrAt && m_url != null) {
				shareIntent.putExtra(Intent.EXTRA_TEXT, m_url);
			} else if (m_viewModel.getBoardType() == SubjectListFragment.BOARD_TYPE_SUBJECT) {
				shareIntent.putExtra(
						Intent.EXTRA_TEXT,
						subject.getTitle() + " http://m.newsmth.net/article/"
								+ subject.getBoardEngName() + "/"
								+ subject.getSubjectID());
			} else if (m_viewModel.getBoardType() == SubjectListFragment.BOARD_TYPE_NORMAL) {
				shareIntent.putExtra(
						Intent.EXTRA_TEXT,
						subject.getTitle()
								+ "http://www.newsmth.net/bbscon.php?bid="
								+ subject.getBoardID() + "&id="
								+ subject.getSubjectID());
			} else if (m_viewModel.getBoardType() == SubjectListFragment.BOARD_TYPE_DIGEST) {
				shareIntent.putExtra(Intent.EXTRA_TEXT,
						subject.getTitle() + " http://m.newsmth.net/article/"
								+ subject.getBoardEngName() + "/single/"
								+ subject.getSubjectID() + "/1");
			} else if (m_viewModel.getBoardType() == SubjectListFragment.BOARD_TYPE_MARK) {
				shareIntent.putExtra(Intent.EXTRA_TEXT,
						subject.getTitle() + " http://m.newsmth.net/article/"
								+ subject.getBoardEngName() + "/single/"
								+ subject.getSubjectID() + "/3");
			}

		}
		return shareIntent;
	}

	private void forwardToEmail(final Post post, final boolean group) {
		String email = aSMApplication.getCurrentApplication()
				.getForwardEmailAddr();
		if (email == "") {
			final EditText input = new EditText(getActivity());

			new AlertDialog.Builder(getActivity())
					.setTitle("设置转寄邮箱")
					.setMessage("您还没有设置转寄邮箱，请先设置。如需更改，请至设置页面")
					.setView(input)
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									Editable value = input.getText();
									aSMApplication.getCurrentApplication()
											.updateForwardEmailAddr(
													value.toString());

									ForwardPostToMailTask task;
									if (group)
										task = new ForwardPostToMailTask(
												getActivity(),
												m_viewModel,
												post,
												ForwardPostToMailTask.FORWARD_TO_EMAIL_GROUP,
												value.toString());
									else
										task = new ForwardPostToMailTask(
												getActivity(),
												m_viewModel,
												post,
												ForwardPostToMailTask.FORWARD_TO_EMAIL,
												value.toString());
									task.execute();
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// Do nothing.
								}
							}).show();
		} else {
			ForwardPostToMailTask task;
			if (group)
				task = new ForwardPostToMailTask(getActivity(), m_viewModel,
						post, ForwardPostToMailTask.FORWARD_TO_EMAIL_GROUP,
						email);
			else
				task = new ForwardPostToMailTask(getActivity(), m_viewModel,
						post, ForwardPostToMailTask.FORWARD_TO_EMAIL, email);
			task.execute();
		}

		return;
	}

	@Override
	public void onNavigationAction(NavigationAction navAction) {
		
		boolean isNext = false;
		if (m_viewModel.getBoardType() == 0) { // 同主题导航

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
				isNext = true;
			}

			m_viewModel.updateSubjectCurrentPageNumberFromCurrentPageNumber();
			m_pageNavigationView.setPageNumberText(m_viewModel.getCurrentPageNumber() + "");
//			if (view.getParent() != null) {
//				((View) view.getParent()).requestFocus();
//			}

			LoadPostTask loadPostTask = new LoadPostTask(m_viewModel,
					m_viewModel.getCurrentSubject(), 0, false, isNext,
					m_startNumber, null);
			loadPostTask.execute();
		} else {
			int action = 0;
			if (navAction == NavigationAction.GoFirst) {
				action = 1;
			} else if (navAction == NavigationAction.GoPrev) {
				action = 2;
			} else if (navAction == NavigationAction.GoNext) {
				action = 3;
				isNext = true;
			} else if (navAction == NavigationAction.GoLast) {
				m_viewModel.setSubjectExpand(true);
				m_viewModel.setBoardType(0);
				m_startNumber = Integer.parseInt(m_viewModel
						.getCurrentSubject().getSubjectID());
				m_viewModel.updateSubjectIDFromTopicSubjectID();
				m_viewModel.setSubjectCurrentPageNumber(1);
			}
			LoadPostTask loadPostTask = new LoadPostTask(m_viewModel,
					m_viewModel.getCurrentSubject(), action, false, isNext,
					m_startNumber, null);
			loadPostTask.execute();
		}
		
		if (m_progressDialogProvider != null) {
			m_progressDialogProvider.showProgressDialog();
		}
		
	}

}
