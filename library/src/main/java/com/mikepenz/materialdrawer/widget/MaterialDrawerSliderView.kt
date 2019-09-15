package com.mikepenz.materialdrawer.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.expandable.ExpandableExtension
import com.mikepenz.fastadapter.expandable.ExpandableExtensionFactory
import com.mikepenz.fastadapter.extensions.ExtensionsFactories
import com.mikepenz.fastadapter.select.SelectExtension
import com.mikepenz.fastadapter.select.SelectExtensionFactory
import com.mikepenz.fastadapter.select.getSelectExtension
import com.mikepenz.fastadapter.utils.DefaultIdDistributor
import com.mikepenz.fastadapter.utils.DefaultIdDistributorImpl
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.Drawer.Companion.BUNDLE_DRAWER_CONTENT_SWITCHED
import com.mikepenz.materialdrawer.Drawer.Companion.BUNDLE_SELECTION
import com.mikepenz.materialdrawer.Drawer.Companion.BUNDLE_STICKY_FOOTER_SELECTION
import com.mikepenz.materialdrawer.DrawerUtils
import com.mikepenz.materialdrawer.DrawerUtils.handleHeaderView
import com.mikepenz.materialdrawer.DrawerUtils.rebuildStickyFooterView
import com.mikepenz.materialdrawer.R
import com.mikepenz.materialdrawer.holder.DimenHolder
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.*
import com.mikepenz.materialdrawer.model.interfaces.*
import com.mikepenz.materialdrawer.util.DrawerUIUtils
import com.mikepenz.materialdrawer.util.ImageHolder
import com.mikepenz.materialize.view.OnInsetsCallback
import java.util.*

/**
 *
 */
open class MaterialDrawerSliderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = R.attr.materialDrawerStyle) : RelativeLayout(context, attrs, defStyleAttr) {

    var insetForeground: Drawable? = null
    internal var insets: Rect? = null
    private val tempRect = Rect()

    var onInsetsCallback: OnInsetsCallback? = null

    var tintStatusBar = false
    var tintNavigationBar = true
    var systemUIVisible = true

    internal var currentStickyFooterSelection = -1

    var savedInstanceKey: String = ""

    // the activity to use
    var layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
    val idDistributor: DefaultIdDistributor<IDrawerItem<*>> = DefaultIdDistributorImpl()

    //defines if we want a inner shadow (used in with the MiniDrawer)
    private var innerShadow = false

    //the account selection header to use
    var accountHeader: AccountHeaderView? = null
        set(value) {
            field = value
        }
    var accountHeaderSticky = false

    // defines if the drawer should scroll to top after click
    var scrollToTopAfterClick = false

    // header view
    var headerView: View? = null
    var headerDivider = true
    var headerPadding = true
    var headerHeight: DimenHolder? = null

    // sticky view
    var stickyHeaderView: View? = null
        set(value) {
            field = value
            handleHeaderView(this)
        }
    // shadow shown on the top of the sticky header
    var stickyHeaderShadow = true
        set(value) {
            field = value
            handleHeaderView(this)
        }

    // footer view
    var footerView: View? = null
    var footerDivider = true
    var footerClickable = false

    // sticky view
    var stickyFooterView: ViewGroup? = null
    // divider shown on top of the sticky footer
    var stickyFooterDivider = false
    // sticky view
    var stickyFooterShadowView: View? = null
    // shadow shown on the top of the sticky footer
    var stickyFooterShadow = true

    // fire onClick after build
    var fireInitialOnClick = false

    // if multiSelection is possible
    var multiSelect
        set(value) {
            this.selectExtension.multiSelect = value
            this.selectExtension.selectOnLongClick = !value
            this.selectExtension.allowDeselection = value
        }
        get() = this.selectExtension.multiSelect

    // item to select
    var selectedItemPosition = 0

    // item to select
    var selectedItemIdentifier: Long = 0

    // the drawerLayout owning this slider
    var drawerLayout: DrawerLayout? = null

    // an RecyclerView to use within the drawer :D
    lateinit var recyclerView: RecyclerView

    // if the adapter should enable hasStableIds to improve performance and allow animations
    var hasStableIds = false

    // an adapter to use for the list
    internal lateinit var _adapter: FastAdapter<IDrawerItem<*>>
    var headerAdapter: ModelAdapter<IDrawerItem<*>, IDrawerItem<*>> = ItemAdapter()
    var itemAdapter: ModelAdapter<IDrawerItem<*>, IDrawerItem<*>> = ItemAdapter()
    var footerAdapter: ModelAdapter<IDrawerItem<*>, IDrawerItem<*>> = ItemAdapter()
    lateinit var expandableExtension: ExpandableExtension<IDrawerItem<*>>
    lateinit var selectExtension: SelectExtension<IDrawerItem<*>>

    /**
     * get the adapter (null safe)
     *
     * @return the FastAdapter used with this drawer
     */
    var adapter: FastAdapter<IDrawerItem<*>>
        get() {
            if (!::_adapter.isInitialized) {
                _adapter = FastAdapter.with(listOf(headerAdapter, itemAdapter, footerAdapter))
                _adapter.setHasStableIds(hasStableIds)
                initAdapter()
                this.selectExtension.isSelectable = true
                this.selectExtension.multiSelect = false
                this.selectExtension.allowDeselection = false
            }
            return _adapter
        }
        set(value) {
            _adapter = value
        }

    // Defines a Adapter which wraps the main Adapter used in the RecyclerView to allow extended navigation and other stuff
    var adapterWrapper: RecyclerView.Adapter<*>? = null

    //defines the itemAnimator to be used in conjunction with the RecyclerView
    var itemAnimator: RecyclerView.ItemAnimator = DefaultItemAnimator()

    // close drawer on click
    var closeOnClick = true

    // delay drawer close to prevent lag
    var delayOnDrawerClose = 50

    // delay drawer click event to prevent lag (you should either choose DelayOnDrawerClose or this)
    var delayDrawerClickEvent = 0

    // defines if we want to keep the sticky items visible, upon switching to the profiles
    var keepStickyItemsVisible = false

    // always visible list in drawer
    var stickyDrawerItems: MutableList<IDrawerItem<*>> = ArrayList()

    // onDrawerItemClickListeners
    var onDrawerItemClickListener: Drawer.OnDrawerItemClickListener? = null

    // onDrawerItemClickListeners
    var onDrawerItemLongClickListener: Drawer.OnDrawerItemLongClickListener? = null

    //variables to store and remember the original list of the drawer
    private var originalOnDrawerItemClickListener: Drawer.OnDrawerItemClickListener? = null
    private var originalOnDrawerItemLongClickListener: Drawer.OnDrawerItemLongClickListener? = null
    /**
     * get the original list of drawerItems
     *
     * @return
     */
    var originalDrawerItems: List<IDrawerItem<*>>? = null
        private set
    private var originalDrawerState: Bundle? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.MaterialDrawerSliderView, defStyleAttr, R.style.Widget_MaterialDrawerStyle)
        insetForeground = a.getDrawable(R.styleable.MaterialDrawerSliderView_insetForeground)
        background = a.getDrawable(R.styleable.MaterialDrawerSliderView_materialDrawerBackground)
        a.recycle()
        setWillNotDraw(true) // No need to draw until the insets are adjusted

        adapter
        createContent()

        if (parent != null) {
            drawerLayout = parent as? DrawerLayout
            (layoutParams as DrawerLayout.LayoutParams).also {
                // if this is a drawer from the right, change the margins :D &  set the new params
                it.width = DrawerUIUtils.getOptimalDrawerWidth(context)
                layoutParams = it
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            if (null == this.insets) {
                this.insets = Rect()
            }

            this.insets?.set(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)

            if (headerView == null && accountHeader == null) {
                recyclerView.updatePadding(top = insets.systemWindowInsetTop, bottom = insets.systemWindowInsetBottom)
            }

            setWillNotDraw(insetForeground == null)
            ViewCompat.postInvalidateOnAnimation(this@MaterialDrawerSliderView)
            onInsetsCallback?.onInsetsChanged(insets)
            insets
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        val width = width
        val height = height
        val insets = insets
        val insetForeground = insetForeground
        if (insets != null && insetForeground != null) {
            val sc = canvas.save()
            canvas.translate(scrollX.toFloat(), scrollY.toFloat())

            if (!systemUIVisible) {
                insets.top = 0
                insets.right = 0
                insets.bottom = 0
                insets.left = 0
            }

            // Top
            if (tintStatusBar) {
                tempRect.set(0, 0, width, insets.top)
                insetForeground.bounds = tempRect
                insetForeground.draw(canvas)
            }

            // Bottom
            if (tintNavigationBar) {
                tempRect.set(0, height - insets.bottom, width, height)
                insetForeground.bounds = tempRect
                insetForeground.draw(canvas)
            }
            // Left
            if (tintNavigationBar) {
                tempRect.set(0, insets.top, insets.left, height - insets.bottom)
                insetForeground.bounds = tempRect
                insetForeground.draw(canvas)
            }

            // Right
            if (tintNavigationBar) {
                tempRect.set(width - insets.right, insets.top, width, height - insets.bottom)
                insetForeground.bounds = tempRect
                insetForeground.draw(canvas)
            }

            canvas.restoreToCount(sc)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        insetForeground?.callback = this
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        insetForeground?.callback = null
    }

    /**
     * Set the Bundle (savedInstance) which is passed by the activity.
     * No need to null-check everything is handled automatically
     *
     * @param savedInstance
     * @return
     */
    fun withSavedInstance(savedInstance: Bundle?) {
        // try to restore all saved values again
        savedInstance?.let { si ->
            this.selectExtension.deselect()
            adapter.withSavedInstanceState(si, BUNDLE_SELECTION + savedInstanceKey)
            DrawerUtils.setStickyFooterSelection(this, si.getInt(BUNDLE_STICKY_FOOTER_SELECTION + savedInstanceKey, -1), null)

            //toggle selection list if we were previously on the account list
            if (si.getBoolean(BUNDLE_DRAWER_CONTENT_SWITCHED + savedInstanceKey, false)) {
                accountHeader?.toggleSelectionList(context)
            }
        }

    }

    /**
     * Add a header to the DrawerBuilder ListView defined by a resource.
     *
     * @param headerViewRes
     * @return
     */
    fun withHeader(@LayoutRes headerViewRes: Int) {
        if (headerViewRes != -1) {
            //i know there should be a root, bit i got none here
            this.headerView = LayoutInflater.from(context).inflate(headerViewRes, null, false)
        }
    }

    /**
     * Add a sticky header below the DrawerBuilder ListView defined by a resource.
     *
     * @param stickyHeaderRes
     * @return
     */
    fun withStickyHeader(@LayoutRes stickyHeaderRes: Int) {
        if (stickyHeaderRes != -1) {
            //i know there should be a root, bit i got none here
            this.stickyHeaderView = LayoutInflater.from(context).inflate(stickyHeaderRes, null, false)
        }
    }

    /**
     * Add a footer to the DrawerBuilder ListView defined by a resource.
     *
     * @param footerViewRes
     * @return
     */
    fun withFooter(@LayoutRes footerViewRes: Int) {
        if (footerViewRes != -1) {
            //i know there should be a root, bit i got none here
            this.footerView = LayoutInflater.from(context).inflate(footerViewRes, null, false)
        }
    }

    /**
     * Add a sticky footer below the DrawerBuilder ListView defined by a resource.
     *
     * @param stickyFooterRes
     * @return
     */
    fun withStickyFooter(@LayoutRes stickyFooterRes: Int) {
        if (stickyFooterRes != -1) {
            //i know there should be a root, bit i got none here
            this.stickyFooterView = LayoutInflater.from(context).inflate(stickyFooterRes, null, false) as ViewGroup
        }
    }

    /**
     * define this if you want enable hasStableIds for the adapter which is generated.
     * WARNING: only use this if you have set an identifer for all of your items else this could cause
     * many weird things
     *
     * @param hasStableIds
     * @return
     */
    fun withHasStableIds(hasStableIds: Boolean) {
        this.hasStableIds = hasStableIds
        adapter.setHasStableIds(hasStableIds)
    }

    /**
     * Define a custom Adapter which will be used in the drawer
     * NOTE: this is not recommender
     * WARNING: if you do this after adding items you will loose those!
     *
     * @param adapter the FastAdapter to use with this drawer
     * @return this
     */
    fun withAdapter(adaptr: FastAdapter<IDrawerItem<*>>) {
        this.adapter = adaptr
        this.selectExtension = adapter.getOrCreateExtension(SelectExtension::class.java)!! // is definitely not null
        //we have to rewrap as a different FastAdapter was provided
        adapter.addAdapter(0, headerAdapter)
        adapter.addAdapter(1, itemAdapter)
        adapter.addAdapter(2, footerAdapter)
        initAdapter()
    }

    private fun initAdapter() {
        ExtensionsFactories.register(SelectExtensionFactory())
        ExtensionsFactories.register(ExpandableExtensionFactory())

        this.selectExtension = adapter.getOrCreateExtension(SelectExtension::class.java)!! // is definitely not null
        headerAdapter.idDistributor = idDistributor
        itemAdapter.idDistributor = idDistributor
        footerAdapter.idDistributor = idDistributor
        expandableExtension = adapter.getOrCreateExtension(ExpandableExtension::class.java)!! // is definitely not null
    }

    /**
     * Defines a Adapter which wraps the main Adapter used in the RecyclerView to allow extended navigation and other stuff
     *
     * @param adapterWrapper
     * @return
     */
    fun withAdapterWrapper(adapterWrapper: RecyclerView.Adapter<*>) {
        if (!::_adapter.isInitialized) {
            throw RuntimeException("this adapter has to be set in conjunction to a normal adapter which is used inside this wrapper adapter")
        }
        this.adapterWrapper = adapterWrapper
    }

    /**
     * Inflates the DrawerItems from a menu.xml
     *
     * @param menuRes
     * @return
     */
    @SuppressLint("RestrictedApi")
    fun inflateMenu(@MenuRes menuRes: Int) {
        val menuInflater = SupportMenuInflater(context)
        val mMenu = MenuBuilder(context)

        menuInflater.inflate(menuRes, mMenu)

        addMenuItems(mMenu, false)
    }

    /**
     * helper method to init the drawerItems from a menu
     *
     * @param mMenu
     * @param subMenu
     */
    private fun addMenuItems(mMenu: Menu, subMenu: Boolean) {
        var groupId = R.id.material_drawer_menu_default_group
        for (i in 0 until mMenu.size()) {
            val mMenuItem = mMenu.getItem(i)
            var iDrawerItem: IDrawerItem<*>
            if (!subMenu && mMenuItem.groupId != groupId && mMenuItem.groupId != 0) {
                groupId = mMenuItem.groupId
                iDrawerItem = DividerDrawerItem()
                itemAdapter.add(iDrawerItem)
            }
            if (mMenuItem.hasSubMenu()) {
                iDrawerItem = PrimaryDrawerItem()
                        .withName(mMenuItem.title.toString())
                        .withIcon(mMenuItem.icon)
                        .withIdentifier(mMenuItem.itemId.toLong())
                        .withEnabled(mMenuItem.isEnabled)
                        .withSelectable(false)
                itemAdapter.add(iDrawerItem)
                addMenuItems(mMenuItem.subMenu, true)
            } else if (mMenuItem.groupId != 0 || subMenu) {
                iDrawerItem = SecondaryDrawerItem()
                        .withName(mMenuItem.title.toString())
                        .withIcon(mMenuItem.icon)
                        .withIdentifier(mMenuItem.itemId.toLong())
                        .withEnabled(mMenuItem.isEnabled)
                itemAdapter.add(iDrawerItem)
            } else {
                iDrawerItem = PrimaryDrawerItem()
                        .withName(mMenuItem.title.toString())
                        .withIcon(mMenuItem.icon)
                        .withIdentifier(mMenuItem.itemId.toLong())
                        .withEnabled(mMenuItem.isEnabled)
                itemAdapter.add(iDrawerItem)
            }
        }
    }

    /**
     * the helper method to create the content for the drawer
     */
    private fun createContent() {
        // if we have an adapter (either by defining a custom one or the included one add a list :D
        val contentView: View
        if (!::recyclerView.isInitialized) {
            contentView = LayoutInflater.from(context).inflate(R.layout.material_drawer_recycler_view, this, false)
            recyclerView = contentView.findViewById(R.id.material_drawer_recycler_view)
            //set the itemAnimator
            recyclerView.itemAnimator = itemAnimator
            //some style improvements on older devices
            recyclerView.setFadingEdgeLength(0)
            recyclerView.clipToPadding = false
            //additional stuff
            recyclerView.layoutManager = layoutManager

        } else {
            contentView = recyclerView
        }

        val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        )
        params.weight = 1f
        this.addView(contentView, params)

        if (innerShadow) {
            val innerShadow = this.findViewById<View>(R.id.material_drawer_inner_shadow)
            innerShadow.visibility = View.VISIBLE
            innerShadow.bringToFront()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && gravity == GravityCompat.END) {
                innerShadow.setBackgroundResource(R.drawable.material_drawer_shadow_right)
            } else {
                innerShadow.setBackgroundResource(R.drawable.material_drawer_shadow_left)
            }
        }

        //handle the header
        DrawerUtils.handleHeaderView(this)

        //handle the footer
        DrawerUtils.handleFooterView(this, OnClickListener { v ->
            val drawerItem = v.getTag(R.id.material_drawer_item) as IDrawerItem<*>
            DrawerUtils.onFooterDrawerItemClick(this, drawerItem, v, true)
        })

        //set the adapter on the listView
        if (adapterWrapper == null) {
            recyclerView.adapter = adapter
        } else {
            recyclerView.adapter = adapterWrapper
        }

        //predefine selection (should be the first element
        if (selectedItemPosition == 0 && selectedItemIdentifier != 0L) {
            selectedItemPosition = DrawerUtils.getPositionByIdentifier(this, selectedItemIdentifier)
        }
        if (headerView != null && selectedItemPosition == 0) {
            selectedItemPosition = 1
        }
        this.selectExtension.deselect()
        this.selectExtension.select(selectedItemPosition)

        // add the onDrawerItemClickListener if set
        adapter.onClickListener = { v: View?, _: IAdapter<IDrawerItem<*>>, item: IDrawerItem<*>, position: Int ->
            if (!(item is Selectable<*> && !item.isSelectable)) {
                resetStickyFooterSelection()
                currentStickyFooterSelection = -1
            }

            //call the listener
            var consumed = false

            //call the item specific listener
            if (item is AbstractDrawerItem<*, *>) {
                consumed = item.onDrawerItemClickListener?.onItemClick(v, position, item) ?: false
            }

            //call the drawer listener
            onDrawerItemClickListener?.let { mOnDrawerItemClickListener ->
                if (delayDrawerClickEvent > 0) {
                    Handler().postDelayed({ mOnDrawerItemClickListener.onItemClick(v, position, item) }, delayDrawerClickEvent.toLong())
                } else {
                    consumed = mOnDrawerItemClickListener.onItemClick(v, position, item)
                }
            }

            //if we were a expandable item we consume the event closing makes no sense
            if (item.subItems.isNotEmpty()) {
                //we consume the event and want no further handling
                true
            } else {
                if (!consumed) {
                    //close the drawer after click
                    closeDrawerDelayed()
                }
                consumed
            }
        }
        // add the onDrawerItemLongClickListener if set
        adapter.onLongClickListener = { v: View, _: IAdapter<IDrawerItem<*>>, item: IDrawerItem<*>, position: Int ->
            onDrawerItemLongClickListener?.onItemLongClick(v, position, item) ?: false
        }

        recyclerView.scrollToPosition(0)

        // call initial onClick event to allow the dev to init the first view
        if (fireInitialOnClick && onDrawerItemClickListener != null) {
            val selection = if (this.selectExtension.selections.isEmpty()) -1 else this.selectExtension.selections.iterator().next()
            adapter.getItem(selection)?.let {
                onDrawerItemClickListener?.onItemClick(null, selection, it)
            }
        }
    }

    /**
     * simple helper method to reset the selection of the sticky footer
     */
    internal fun resetStickyFooterSelection() {
        stickyFooterView?.let {
            if (it is LinearLayout) {
                for (i in 0 until it.childCount) {
                    it.getChildAt(i).isActivated = false
                    it.getChildAt(i).isSelected = false
                }
            }
        }
    }

    /**
     * helper method to close the drawer delayed
     */
    internal fun closeDrawerDelayed() {
        if (closeOnClick && drawerLayout != null) {
            if (delayOnDrawerClose > -1) {
                Handler().postDelayed({
                    drawerLayout?.closeDrawers()

                    if (scrollToTopAfterClick) {
                        recyclerView.smoothScrollToPosition(0)
                    }
                }, delayOnDrawerClose.toLong())
            } else {
                drawerLayout?.closeDrawers()
            }
        }
    }


    /**
     * information if the current drawer content is switched by alternative content (profileItems)
     *
     * @return
     */
    fun switchedDrawerContent(): Boolean {
        return !(originalOnDrawerItemClickListener == null && originalDrawerItems == null && originalDrawerState == null)
    }

    /**
     * method to switch the drawer content to new elements
     *
     * @param onDrawerItemClickListener
     * @param drawerItems
     * @param drawerSelection
     */
    fun switchDrawerContent(onDrawerItemClickListenerInner: Drawer.OnDrawerItemClickListener, onDrawerItemLongClickListenerInner: Drawer.OnDrawerItemLongClickListener, drawerItemsInner: List<IDrawerItem<*>>, drawerSelection: Int) {
        //just allow a single switched drawer
        if (!switchedDrawerContent()) {
            //save out previous values
            originalOnDrawerItemClickListener = onDrawerItemClickListener
            originalOnDrawerItemLongClickListener = onDrawerItemLongClickListener
            originalDrawerState = adapter.saveInstanceState(Bundle())
            expandableExtension.collapse(false)
            originalDrawerItems = itemAdapter.adapterItems
        }

        //set the new items
        onDrawerItemClickListener = onDrawerItemClickListenerInner
        onDrawerItemLongClickListener = onDrawerItemLongClickListenerInner
        setItems(drawerItemsInner, true)
        setSelectionAtPosition(drawerSelection, false)

        if (!keepStickyItemsVisible) {
            //hide stickyFooter and it's shadow
            stickyFooterView?.visibility = View.GONE
            stickyFooterShadowView?.visibility = View.GONE
        }
    }

    /**
     * helper method to reset to the original drawerContent
     */
    fun resetDrawerContent() {
        if (switchedDrawerContent()) {
            //set the new items
            onDrawerItemClickListener = originalOnDrawerItemClickListener
            onDrawerItemLongClickListener = originalOnDrawerItemLongClickListener
            setItems(originalDrawerItems, true)
            adapter.withSavedInstanceState(originalDrawerState)
            //remove the references
            originalOnDrawerItemClickListener = null
            originalOnDrawerItemLongClickListener = null
            originalDrawerItems = null
            originalDrawerState = null

            //if we switch back scroll back to the top
            recyclerView.smoothScrollToPosition(0)

            //show the stickyFooter and it's shadow again
            stickyFooterView?.visibility = View.VISIBLE
            stickyFooterShadowView?.visibility = View.VISIBLE

            //if we currently show the accountHeader selection list make sure to reset this attr
            accountHeader?.selectionListShown = false
        }
    }

    /**
     * replace the current DrawerItems with the new ArrayList.
     *
     * @param drawerItems
     * @param switchedItems
     */
    private fun setItems(drawerItems: List<IDrawerItem<*>>?, switchedItems: Boolean) {
        //if we are currently at a switched list set the new reference
        if (originalDrawerItems != null && !switchedItems) {
            originalDrawerItems = drawerItems
        }
        itemAdapter.setNewList(drawerItems ?: ArrayList())
    }


    /*
     * set the current selection in the drawer
     * NOTE: this also deselects all other selections. if you do not want this. use the direct api of the adater .getAdapter().select(position, fireOnClick)
     * NOTE: This will trigger onDrawerItemSelected without a view if you pass fireOnClick = true;
     *
     * @param position
     * @param fireOnClick
     * @return true if the event was consumed
     */
    @JvmOverloads
    fun setSelectionAtPosition(position: Int, fireOnClick: Boolean = true): Boolean {
        selectExtension.deselect()
        selectExtension.select(position, false)
        notifySelect(position, fireOnClick)
        return false
    }

    private fun notifySelect(position: Int, fireOnClick: Boolean) {
        if (fireOnClick && position >= 0) {
            adapter.getItem(position)?.let { item ->
                if (item is AbstractDrawerItem<*, *>) {
                    item.onDrawerItemClickListener?.onItemClick(null, position, item)
                }
                onDrawerItemClickListener?.onItemClick(null, position, item)
            }
        }

        //we set the selection on a normal item in the drawer so we have to deselect the items in the StickyDrawer
        resetStickyFooterSelection()
    }


    /**
     * method to replace a previous set header
     *
     * @param view
     * @param padding
     * @param divider
     * @param height
     */
    @JvmOverloads
    fun setHeader(view: View?, padding: Boolean, divider: Boolean, height: DimenHolder? = null) {
        headerAdapter.clear()
        view?.let {
            if (padding) {
                headerAdapter.add(ContainerDrawerItem().withView(view).withDivider(divider).withHeight(height).withViewPosition(ContainerDrawerItem.Position.TOP))
            } else {
                headerAdapter.add(ContainerDrawerItem().withView(view).withDivider(divider).withHeight(height).withViewPosition(ContainerDrawerItem.Position.NONE))
            }
        }
        //we need to set the padding so the header starts on top
        recyclerView.setPadding(recyclerView.paddingLeft, 0, recyclerView.paddingRight, recyclerView.paddingBottom)
    }


    /**
     * set the current selection in the drawer
     * NOTE: This will trigger onDrawerItemSelected without a view if you pass fireOnClick = true;
     *
     * @param identifier  the identifier to search for
     * @param fireOnClick true if the click listener should be called
     */
    @JvmOverloads
    fun setSelection(identifier: Long, fireOnClick: Boolean = true) {
        val select = adapter.getSelectExtension()
        select.deselect()
        select.selectByIdentifier(identifier, false, true)

        //we also have to call the general notify
        val res = adapter.getItemById(identifier)
        if (res != null) {
            val position = res.second
            notifySelect(position ?: -1, fireOnClick)
        }
    }

    /**
     * Add a initial DrawerItem or a DrawerItem Array for the StickyDrawerFooter
     *
     * @param stickyDrawerItems
     * @return
     */
    fun addStickyDrawerItems(vararg stickyDrawerItems: IDrawerItem<*>) {
        Collections.addAll(this.stickyDrawerItems, *stickyDrawerItems)
        rebuildStickyFooterView(this)
    }


    /**
     * add the values to the bundle for saveInstanceState
     *
     * @param savedInstanceState
     * @return
     */
    fun saveInstanceState(_savedInstanceState: Bundle): Bundle {
        adapter.saveInstanceState(_savedInstanceState, BUNDLE_SELECTION + savedInstanceKey).apply {
            putInt(BUNDLE_STICKY_FOOTER_SELECTION + savedInstanceKey, currentStickyFooterSelection)
            putBoolean(BUNDLE_DRAWER_CONTENT_SWITCHED + savedInstanceKey, switchedDrawerContent())
        }
        return _savedInstanceState
    }

    /**
     * calculates the position of an drawerItem. searching by it's identifier
     *
     * @param drawerItem
     * @return
     */
    fun getPosition(drawerItem: IDrawerItem<*>): Int {
        return getPosition(drawerItem.identifier)
    }

    /**
     * calculates the position of an drawerItem. searching by it's identifier
     *
     * @param identifier
     * @return
     */
    fun getPosition(identifier: Long): Int {
        return DrawerUtils.getPositionByIdentifier(this, identifier)
    }

    /**
     * returns the DrawerItem by the given identifier
     *
     * @param identifier
     * @return
     */
    fun getDrawerItem(identifier: Long): IDrawerItem<*>? {
        val res = adapter.getItemById(identifier)
        return res?.first
    }

    /**
     * returns the found drawerItem by the given tag
     *
     * @param tag
     * @return
     */
    fun getDrawerItem(tag: Any): IDrawerItem<*>? {
        return DrawerUtils.getDrawerItem(itemAdapter.adapterItems, tag)
    }

    /**
     * update a specific drawer item :D
     * automatically identified by its id
     *
     * @param drawerItem
     */
    fun updateItem(drawerItem: IDrawerItem<*>) {
        updateItemAtPosition(drawerItem, getPosition(drawerItem))
    }

    /**
     * update the badge for a specific drawerItem
     * identified by its id
     *
     * @param identifier
     * @param badge
     */
    fun updateBadge(identifier: Long, badge: StringHolder) {
        val drawerItem = getDrawerItem(identifier)
        if (drawerItem is Badgeable<*>) {
            drawerItem.withBadge(badge)
            updateItem(drawerItem)
        }
    }

    /**
     * update the name for a specific drawerItem
     * identified by its id
     *
     * @param identifier
     * @param name
     */
    fun updateName(identifier: Long, name: StringHolder) {
        val drawerItem = getDrawerItem(identifier)
        if (drawerItem is Nameable<*>) {
            drawerItem.withName(name)
            updateItem(drawerItem)
        }
    }

    /**
     * update the name for a specific drawerItem
     * identified by its id
     *
     * @param identifier
     * @param image
     */
    fun updateIcon(identifier: Long, image: ImageHolder) {
        val drawerItem = getDrawerItem(identifier)
        if (drawerItem is Iconable<*>) {
            drawerItem.withIcon(image)
            updateItem(drawerItem)
        }
    }

    /**
     * Update a drawerItem at a specific position
     *
     * @param drawerItem
     * @param position
     */
    fun updateItemAtPosition(drawerItem: IDrawerItem<*>, position: Int) {
        if (checkDrawerItem(position, false)) {
            itemAdapter[position] = drawerItem
        }
    }

    /**
     * check if the item is within the bounds of the list
     *
     * @param position
     * @param includeOffset
     * @return
     */
    internal fun checkDrawerItem(position: Int, includeOffset: Boolean): Boolean {
        return adapter.getItem(position) != null
    }
}
