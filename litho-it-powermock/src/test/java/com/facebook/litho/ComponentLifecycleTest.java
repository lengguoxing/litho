/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.yoga.YogaMeasureMode.EXACTLY;
import static junit.framework.Assert.fail;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import android.content.res.Resources;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaMeasureOutput;
import com.facebook.yoga.YogaNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

/**
 * Tests {@link ComponentLifecycle}
 */
@PrepareForTest({
    InternalNode.class,
    DiffNode.class,
    LayoutState.class,
    ComponentsPools.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@RunWith(ComponentsTestRunner.class)
public class ComponentLifecycleTest {

  @Rule
  public PowerMockRule mPowerMockRule = new PowerMockRule();

  private static final int A_HEIGHT = 11;
  private static final int A_WIDTH = 12;
  private int mNestedTreeWidthSpec;
  private int mNestedTreeHeightSpec;

  private InternalNode mNode;
  private DiffNode mDiffNode;
  private Component mInput;
  private ComponentContext mContext;
  private Component mComponentWithNullLayout;

  @Before
  public void setUp() {
    mDiffNode = mock(DiffNode.class);
    mNode = mock(InternalNode.class);
    final YogaNode cssNode = new YogaNode();
    cssNode.setData(mNode);
    mNode.mYogaNode = cssNode;

    mockStatic(ComponentsPools.class);

    when(mNode.getLastWidthSpec()).thenReturn(-1);
    when(mNode.getDiffNode()).thenReturn(mDiffNode);
    when(mDiffNode.getLastMeasuredWidth()).thenReturn(-1f);
    when(mDiffNode.getLastMeasuredHeight()).thenReturn(-1f);
    when(ComponentsPools.acquireInternalNode(any(ComponentContext.class), any(Resources.class)))
        .thenReturn(mNode);
    mInput = mock(Component.class);

    mockStatic(LayoutState.class);
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mComponentWithNullLayout = new InlineLayoutSpec() {
      @Override protected ComponentLayout onCreateLayout(ComponentContext c) { return null; }
    };
    mNestedTreeWidthSpec = SizeSpec.makeSizeSpec(400, SizeSpec.EXACTLY);
    mNestedTreeHeightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);
  }

  @Test
  public void testCreateLayoutWithNullComponentWithLayoutSpecCannotMeasure() {
    ComponentLifecycle componentLifecycle = setUpComponentForCreateLayout(
        false /* isMountSpec */,
        false /* canMeasure */);
    componentLifecycle.createLayout(mContext, mComponentWithNullLayout, false);

    verify(componentLifecycle).onCreateLayout(mContext, mComponentWithNullLayout);
    verify(mNode).appendComponent(mComponentWithNullLayout);
    verify(componentLifecycle).onPrepare(mContext, mComponentWithNullLayout);
  }

  @Test
  public void testCreateLayoutWithNullComponentWithLayoutSpecCanMeasure() {
    ComponentLifecycle componentLifecycle = setUpComponentForCreateLayout(
        false /* isMountSpec */,
        true /* canMeasure */);
    componentLifecycle.createLayout(mContext, mComponentWithNullLayout, false);

    verify(componentLifecycle).onCreateLayout(mContext, mComponentWithNullLayout);
    verify(mNode).appendComponent(mComponentWithNullLayout);
    verify(componentLifecycle).onPrepare(mContext, mComponentWithNullLayout);
  }

  @Test
  public void testCreateLayoutWithNullComponentWithMountSpecCannotMeasure() {
    ComponentLifecycle componentLifecycle = setUpComponentForCreateLayout(
        true /* isMountSpec */,
        false /* canMeasure */);
    componentLifecycle.createLayout(mContext, mComponentWithNullLayout, false);

    verify(componentLifecycle).onCreateLayout(mContext, mComponentWithNullLayout);
    verify(mNode).appendComponent(mComponentWithNullLayout);
    verify(componentLifecycle).onPrepare(mContext, mComponentWithNullLayout);
  }

  @Test
  public void testCreateLayoutWithNullComponentWithMountSpecCanMeasure() {
    ComponentLifecycle componentLifecycle = setUpComponentForCreateLayout(
        true /* isMountSpec */,
        true /* canMeasure */);
    componentLifecycle.createLayout(mContext, mComponentWithNullLayout, false);

    verify(componentLifecycle).onCreateLayout(mContext, mComponentWithNullLayout);
    verify(mNode).appendComponent(mComponentWithNullLayout);
    verify(componentLifecycle).onPrepare(mContext, mComponentWithNullLayout);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithMountSpecCannotMeasure() {
    ComponentLifecycle componentLifecycle = setUpComponentForCreateLayout(
        true /* isMountSpec */,
        false /* canMeasure */);
    componentLifecycle.createLayout(mContext, mInput, true);

    verify(componentLifecycle).onCreateLayout(mContext, mInput);
    verify(mNode).appendComponent(mInput);
    verify(mNode, never()).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(componentLifecycle).onPrepare(mContext, mInput);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithMountSpecCannotMeasure() {
    ComponentLifecycle componentLifecycle = setUpComponentForCreateLayout(
        true /* isMountSpec */,
        false /* canMeasure */);
    componentLifecycle.createLayout(mContext, mInput, false);

    verify(componentLifecycle).onCreateLayout(mContext, mInput);
    verify(mNode).appendComponent(mInput);
    verify(mNode, never()).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(componentLifecycle).onPrepare(mContext, mInput);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithMountSpecCanMeasure() {
    ComponentLifecycle componentLifecycle = setUpComponentForCreateLayout(
        true /* isMountSpec */,
        true /* canMeasure */);
    componentLifecycle.createLayout(mContext, mInput, true);

    verify(componentLifecycle).onCreateLayout(mContext, mInput);
    verify(mNode).appendComponent(mInput);
    verify(mNode).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(componentLifecycle).onPrepare(mContext, mInput);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithMountSpecCanMeasure() {
    ComponentLifecycle componentLifecycle = setUpComponentForCreateLayout(
        true /* isMountSpec */,
        true /* canMeasure */);
    componentLifecycle.createLayout(mContext, mInput, false);

    verify(componentLifecycle).onCreateLayout(mContext, mInput);
    verify(mNode).appendComponent(mInput);
    verify(mNode).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(componentLifecycle).onPrepare(mContext, mInput);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithLayoutSpecCannotMeasure() {
    ComponentLifecycle componentLifecycle = setUpComponentForCreateLayout(
        false /* isMountSpec */,
        false /* canMeasure */);
    componentLifecycle.createLayout(mContext, mInput, true);

    verify(componentLifecycle).onCreateLayout(mContext, mInput);
    verify(mNode).appendComponent(mInput);
    verify(mNode, never()).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(componentLifecycle).onPrepare(mContext, mInput);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithLayoutSpecCannotMeasure() {
    ComponentLifecycle componentLifecycle = setUpComponentForCreateLayout(
        false /* isMountSpec */,
        false /* canMeasure */);
    componentLifecycle.createLayout(mContext, mInput, false);

    verify(componentLifecycle).onCreateLayout(mContext, mInput);
    verify(mNode).appendComponent(mInput);
    verify(mNode, never()).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(componentLifecycle).onPrepare(mContext, mInput);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithLayoutSpecCanMeasure() {
    ComponentLifecycle componentLifecycle = setUpComponentForCreateLayout(
        false /* isMountSpec */,
        true /* canMeasure */);
    mContext.setWidthSpec(mNestedTreeWidthSpec);
    mContext.setHeightSpec(mNestedTreeHeightSpec);
    componentLifecycle.createLayout(mContext, mInput, true);

    verify(componentLifecycle).onCreateLayoutWithSizeSpec(
        mContext,
        mNestedTreeWidthSpec,
        mNestedTreeHeightSpec,
        mInput);
    verify(mNode).appendComponent(mInput);
    verify(mNode, never()).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(componentLifecycle).onPrepare(mContext, mInput);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithLayoutSpecCanMeasure() {
    ComponentLifecycle componentLifecycle = setUpComponentForCreateLayout(
        false /* isMountSpec */,
        true /* canMeasure */);
    componentLifecycle.createLayout(mContext, mInput, false);

    PowerMockito.verifyStatic();
    // Calling here to verify static call.
    ComponentsPools.acquireInternalNode(mContext, mContext.getResources());
    verify(componentLifecycle, never()).onCreateLayout(
        any(ComponentContext.class),
        any(Component.class));
    verify(componentLifecycle, never()).onCreateLayoutWithSizeSpec(
        any(ComponentContext.class),
        anyInt(),
        anyInt(),
        any(Component.class));
    verify(mNode).appendComponent(mInput);
    verify(mNode).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(componentLifecycle, never())
        .onPrepare(any(ComponentContext.class), any(Component.class));
  }

  @Test
  public void testOnMeasureNotOverriden() {
    setUpComponentForCreateLayout(true, true);
    YogaMeasureFunction measureFunction = getMeasureFunction();

    try {
      measureFunction.measure(mNode.mYogaNode, 0, EXACTLY, 0, EXACTLY);
      fail();
    } catch (Exception e) {
      assertThat(e).isExactlyInstanceOf(IllegalStateException.class);
      assertThat(e.getMessage()).contains("canMeasure()");
    }
  }

  @Test
  public void testMountSpecYogaMeasureOutputNotSet() {
    ComponentLifecycle componentLifecycle = new TestMountSpecWithEmptyOnMeasure();
    YogaMeasureFunction measureFunction = getMeasureFunction();
    when(mInput.getLifecycle()).thenReturn(componentLifecycle);
    Whitebox.setInternalState(mInput, "mLifecycle", componentLifecycle);

    try {
      measureFunction.measure(mNode.mYogaNode, 0, EXACTLY, 0, EXACTLY);
      fail();
    } catch (Exception e) {
      assertThat(e).isExactlyInstanceOf(IllegalStateException.class);
      assertThat(e.getMessage()).contains("MeasureOutput not set");
    }
  }

  @Test
  public void testMountSpecYogaMeasureOutputSet() {
    ComponentLifecycle componentLifecycle = new TestMountSpecSettingSizesInOnMeasure();
    YogaMeasureFunction measureFunction = getMeasureFunction();
    when(mInput.getLifecycle()).thenReturn(componentLifecycle);
    Whitebox.setInternalState(mInput, "mLifecycle", componentLifecycle);

    long output = measureFunction.measure(mNode.mYogaNode, 0, EXACTLY, 0, EXACTLY);

    assertThat(YogaMeasureOutput.getWidth(output)).isEqualTo(A_WIDTH);
    assertThat(YogaMeasureOutput.getHeight(output)).isEqualTo(A_HEIGHT);
  }

  @Test
  public void testLayoutSpecMeasureResolveNestedTree() {
    setUpComponentForCreateLayout(false /* isMountSpec */, true /* canMeasure */);
    YogaMeasureFunction measureFunction = getMeasureFunction();

    final int nestedTreeWidth = 20;
    final int nestedTreeHeight = 25;
    InternalNode nestedTree = mock(InternalNode.class);
    when(nestedTree.getWidth()).thenReturn(nestedTreeWidth);
    when(nestedTree.getHeight()).thenReturn(nestedTreeHeight);

    when(LayoutState.resolveNestedTree(eq(mNode), anyInt(), anyInt())).thenReturn(nestedTree);

    long output = measureFunction.measure(mNode.mYogaNode, 0, EXACTLY, 0, EXACTLY);

    PowerMockito.verifyStatic();
    LayoutState.resolveNestedTree(eq(mNode), anyInt(), anyInt());

    assertThat(YogaMeasureOutput.getWidth(output)).isEqualTo(nestedTreeWidth);
    assertThat(YogaMeasureOutput.getHeight(output)).isEqualTo(nestedTreeHeight);
  }

  private ComponentLifecycle setUpComponentForCreateLayout(
      boolean isMountSpec,
      boolean canMeasure) {
    ComponentLifecycle componentLifecycle = spy(new TestBaseComponent());
    when(componentLifecycle.canMeasure()).thenReturn(canMeasure);
    when(componentLifecycle.getMountType()).thenReturn(
        isMountSpec ? ComponentLifecycle.MountType.DRAWABLE : ComponentLifecycle.MountType.NONE);
    when(mInput.getLifecycle()).thenReturn(componentLifecycle);

    Whitebox.setInternalState(mInput, "mLifecycle", componentLifecycle);

    return componentLifecycle;
  }

  private YogaMeasureFunction getMeasureFunction() {
    when(mNode.getRootComponent()).thenReturn(mInput);

    return Whitebox.getInternalState(ComponentLifecycle.class, "sMeasureFunction");
  }

  private class TestBaseComponent extends ComponentLifecycle {
    @Override
    protected ComponentLayout onCreateLayout(ComponentContext c, Component<?> input) {
      return mNode;
    }

    @Override
    protected ComponentLayout onCreateLayoutWithSizeSpec(
        ComponentContext c, int widthSpec, int heightSpec, Component<?> component) {
      return mNode;
    }
  }

  private class TestMountSpecWithEmptyOnMeasure extends TestBaseComponent {

    @Override
    public MountType getMountType() {
      return MountType.DRAWABLE;
    }

    @Override
    protected boolean canMeasure() {
      return true;
    }

    @Override
    protected void onMeasure(
        ComponentContext c,
        ComponentLayout layout,
        int widthSpec,
        int heightSpec,
        Size size,
        Component<?> component) {
    }
  }

  private class TestMountSpecSettingSizesInOnMeasure extends TestMountSpecWithEmptyOnMeasure {

    @Override
    protected void onMeasure(
        ComponentContext context,
        ComponentLayout layout,
        int widthSpec,
        int heightSpec,
        Size size,
        Component<?> component) {
      size.width = A_WIDTH;
      size.height = A_HEIGHT;
    }
  }
}
