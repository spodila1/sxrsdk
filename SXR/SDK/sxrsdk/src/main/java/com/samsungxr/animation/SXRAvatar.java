package com.samsungxr.animation;

import com.samsungxr.IAssetEvents;
import com.samsungxr.IEventReceiver;
import com.samsungxr.IEvents;
import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRBehavior;
import com.samsungxr.SXRComponent;
import com.samsungxr.SXRContext;
import com.samsungxr.SXREventReceiver;
import com.samsungxr.SXRImportSettings;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRResourceVolume;
import com.samsungxr.SXRTexture;
import com.samsungxr.animation.keyframe.BVHImporter;
import com.samsungxr.animation.keyframe.SXRSkeletonAnimation;
import com.samsungxr.utility.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Group of animations that can be collectively manipulated.
 *
 * Typically the animations belong to a particular model and
 * represent a sequence of poses for the model over time.
 * This class allows you to start, stop and set animation modes
 * for all the animations in the group at once.
 * An asset which has animations will have this component
 * attached to collect the animations for the asset.
 *
 * @see com.samsungxr.SXRAssetLoader
 * @see com.samsungxr.SXRExternalScene
 * @see SXRAvatar
 * @see SXRAnimationEngine
 */
public class SXRAvatar extends SXRBehavior implements IEventReceiver
{
    private static final String TAG = Log.tag(SXRAvatar.class);
    static private long TYPE_AVATAR = newComponentType(SXRAvatar.class);
    protected final List<SXRAnimator> mAnimations;
    protected SXRSkeleton mSkeleton;
    protected final SXRNode mAvatarRoot;
    protected boolean mIsRunning;
    protected SXREventReceiver mReceiver;
    protected final List<SXRAnimator> mAnimQueue = new ArrayList<SXRAnimator>();
    protected int mRepeatMode = SXRRepeatMode.ONCE;
    protected int mRepeatCount = 1;
    private float repeatCounter = 0;
    private boolean reverse = false;
    private float mBlendFactor = 0;
    private String mBoneMap = "";
    private SXRContext mContext;
    private boolean mBlend = false;
    private boolean dummy = false;
    private boolean order = false;
    public enum mAnimationsOrder
    {
        FIRST, MIDDLE, LAST;
    }

    /**
     * Make an instance of the SXRAnimator component.
     * Auto-start is not enabled - a call to start() is
     * required to run the animations.
     *
     * @param ctx SXRContext for this avatar
     */
    public SXRAvatar(SXRContext ctx, String name)
    {
        super(ctx);
        mContext = ctx;
        mReceiver = new SXREventReceiver(this);
        mType = getComponentType();
        mAvatarRoot = new SXRNode(ctx);
        mAvatarRoot.setName(name);
        mAnimations = new CopyOnWriteArrayList<>();
    }

    static public long getComponentType() { return TYPE_AVATAR; }

    /**
     * Get the event receiver for this avatar.
     * <p>
     * The avatar will generate events when assets are loaded,
     * animations are started or finished. Clients can observe
     * these events by attaching IAvatarEvent listeners to
     * this event receiver.
     */
    public SXREventReceiver getEventReceiver() { return mReceiver; }

    /**
     * Get the name of this avatar (supplied at construction time).
     * @returns string with avatar name
     */
    public String getName() { return mAvatarRoot.getName(); }

    /**
     * Get the skeleton for the avatar.
     * <p>
     * The skeleton is part of the avatar model. When the asset loader loads
     * the avatar model, the skeleton should be part of the asset.
     * @return skeleton associated with the avatar
     */
    public  SXRSkeleton getSkeleton() { return mSkeleton; }

    /**
     * Get the root of the node hierarchy for the avatar.
     * <p>
     * The avatar model is constructed by the asset loader when the avatar
     * model is loaded. It contains the scene hierarchy with the skeleton
     * bones and the meshes for the avatar.
     * @return root of the avatar model hierarchy
     */
    public SXRNode getModel() { return mAvatarRoot; }

    /**
     * Determine if this avatar is currently animating.
     */
    public boolean isRunning() { return mIsRunning; }

    /**
     * Query the number of animations owned by this avatar.
     * @return number of animations added to this avatar
     */
    public int getAnimationCount()
    {
        return mAnimations.size();
    }

    /**
     * Sets the blend and blend duration.
     * @param blend true to apply blend; false no blend.
     * @param blendFactor duration of blend.
     */
    public void setBlend(boolean blend, float blendFactor)
    {
        mBlend = blend;
        mBlendFactor = blendFactor;
    }

    protected SXROnFinish mOnFinish = new SXROnFinish()
    {
        public void finished(SXRAnimation animation)
        {
            int numEvents = 0;
            SXRAnimator animator = null;
            synchronized (mAnimQueue)
            {
                if (mAnimQueue.size() > 0)
                {
                    animator = mAnimQueue.get(0);

                    if (animator.findAnimation(animation) >= 0)
                    {
                        if(mBlend)
                        {
                            if(!order)
                            {
                              orderAnimations(); //order the animations for Avatar
                            }

                            //For skeleton animation add interpolator
                            if(mAnimQueue.get(0).getAnimation(0).getClass().getName().contains("SXRSkeletonAnimation")
                                    && mAnimQueue.size()>=2)
                            {
                                addAnimationInterpolator(0,1, mBlendFactor, 1);
                            }
                        }

                        mAnimQueue.remove(0);
                        mIsRunning = false;

                        if (mAnimQueue.size() > 0)
                        {
                            animator = mAnimQueue.get(0);
                            animator.setBlend(mBlend, mBlendFactor);
                            animator.start(mOnFinish);
                            numEvents = 2;
                        }
                        else
                        {
                            numEvents = 1;
                        }
                    }
                }
            }
            switch (numEvents)
            {
                case 2:
                    mAvatarRoot.getSXRContext().getEventManager().sendEvent(SXRAvatar.this,
                            IAvatarEvents.class,
                            "onAnimationFinished",
                            SXRAvatar.this,
                            animator,
                            animation);
                    mAvatarRoot.getSXRContext().getEventManager().sendEvent(SXRAvatar.this,
                            IAvatarEvents.class,
                            "onAnimationStarted",
                            SXRAvatar.this,
                            animator);

                    break;

                case 1:
                    mAvatarRoot.getSXRContext().getEventManager().sendEvent(SXRAvatar.this,
                            IAvatarEvents.class,
                            "onAnimationStarted",
                            SXRAvatar.this,
                            animator);

                    //REPEATED

                    if (mRepeatMode == SXRRepeatMode.REPEATED)
                    {
                        repeatCounter++;

                        if(repeatCounter < mRepeatCount || mRepeatCount < 0){
                            startAll(mRepeatMode, mRepeatCount);
                        }

                    }

                    //PINGPONG

                    if (mRepeatMode == SXRRepeatMode.PINGPONG)
                    {
                        if(!dummy)
                        {
                            //play dummy animation till stillRunning set to false for previous animation in mAnimQueue
                            playDummyAnimation(animator);
                        }
                        else
                        {
                            repeatCounter = repeatCounter + 0.5f ; //increment in halves for PINGPONG

                            if(repeatCounter < mRepeatCount || mRepeatCount<0) {

                                reverse = !reverse;
                                Collections.reverse(mAnimations); //reverse the order of animations in the list

                                mAnimationsOrder temp = mAnimations.get(0).getAnimation(0).getAnimationOrder();
                                mAnimations.get(0).setAnimationOrder(mAnimations.get(mAnimations.size()-1).getAnimation(0).getAnimationOrder());
                                mAnimations.get(mAnimations.size()-1).setAnimationOrder(temp);  //alter the order names FIRST to LAST & LAST to FIRST

                            for (SXRAnimator anim : mAnimations)
                            {
                                anim.setRepeatCount(1); //set default value 1 for SXRAnimation
                                anim.setRepeatMode(mRepeatMode);
                                anim.setReverse(reverse);
                            }

                            startAll(mRepeatMode, mRepeatCount);

                            }
                            dummy =false;

                        }

                    }

                default: break;
            }
        }
    };

    /**
     * Assign order name to the animations in avatar: FIRST, MIDDLE, and LAST
     */
    private void orderAnimations()
    {
        for(int i=1; i<mAnimQueue.size()-1; i++)
        {

            mAnimQueue.get(i).setAnimationOrder(mAnimationsOrder.MIDDLE);
        }

        mAnimQueue.get(mAnimQueue.size()-1).setAnimationOrder(mAnimationsOrder.LAST);
        order = true;
    }

    /**
     * Add blend of skeleton animation to the avatar
     * @param previous index of previous animation in mAnimQueue
     * @param next     index of next animation in mAnimQueue
     * @param duration duration of blend animation
     * @param position add blend animation to the given position in mAnimQueue
     */
    public void addAnimationInterpolator(int previous, int next, float duration, int position)
    {
        SXRSkeletonAnimation skelOne = (SXRSkeletonAnimation)mAnimQueue.get(previous).getAnimation(0);
        SXRSkeletonAnimation skelTwo = (SXRSkeletonAnimation)mAnimQueue.get(next).getAnimation(0);

        SXRPoseInterpolator blendAnim = new SXRPoseInterpolator(getModel(), duration, skelOne, skelTwo, skelOne.getSkeleton(), reverse);
        SXRPoseMapper retargeterP = new SXRPoseMapper(getSkeleton(), skelOne.getSkeleton(), duration);
        retargeterP.setBoneMap(mBoneMap);

        SXRAnimator temp = new SXRAnimator(mContext);
        temp.addAnimation(blendAnim);
        temp.addAnimation(retargeterP);

        mAnimQueue.add(position,temp);
    }

    /**
     * Add interpolation of pose animation to the avatar
     * @param previous index of previous animation in mAnimQueue
     * @param next     index of next animation in mAnimQueue
     * @param duration duration of blend animation
     * @param position add blend animation to the given position in mAnimQueue
     */
    public void addPoseInterpolator(int previous, int next, float duration, int position)
    {
        SXRSkeletonAnimation skelAnim = (SXRSkeletonAnimation)mAnimations.get(previous).getAnimation(0);
        SXRPose poseOne = ((SXRSkeletonAnimation) mAnimations.get(previous).getAnimation(0)).getSkeleton().getPose();
        SXRPose poseTwo = ((SXRSkeletonAnimation) mAnimations.get(next).getAnimation(0)).getSkeleton().getPose();

        SXRPoseInterpolator blendAnim = new SXRPoseInterpolator(getModel(), duration, poseOne, poseTwo, skelAnim.getSkeleton());
        SXRPoseMapper retargeterP = new SXRPoseMapper(getSkeleton(), skelAnim.getSkeleton(), duration);

        retargeterP.setBoneMap(mBoneMap);
        SXRAnimator temp = new SXRAnimator(mContext);
        temp.addAnimation(blendAnim);
        temp.addAnimation(retargeterP);

        mAnimQueue.add(position,temp);
    }

    /**
     * Play dummy animation to delay start playing next animation
     * @param animator finished animation
     */
    private void playDummyAnimation(SXRAnimator animator)
    {
        addPoseInterpolator(mAnimations.size()-1, mAnimations.size()-1, 0.1f, 0);
        animator = mAnimQueue.get(0);
        animator.start(mOnFinish);
        dummy = true;
    }
    /**
     * Load the avatar base model
     * @param avatarResource    resource with avatar model
     */
    public void loadModel(SXRAndroidResource avatarResource)
    {
        EnumSet<SXRImportSettings> settings = SXRImportSettings.getRecommendedSettingsWith(EnumSet.of(SXRImportSettings.OPTIMIZE_GRAPH, SXRImportSettings.NO_ANIMATION));
        SXRContext ctx = mAvatarRoot.getSXRContext();
        SXRResourceVolume volume = new SXRResourceVolume(ctx, avatarResource);
        SXRNode modelRoot = new SXRNode(ctx);

        ctx.getAssetLoader().loadModel(volume, modelRoot, settings, false, mLoadModelHandler);
    }

    /**
     * Load a model to attach to the avatar
     * @param avatarResource    resource with avatar model
     * @param attachBone        name of bone to attach model to
     */
    public void loadModel(SXRAndroidResource avatarResource, String attachBone)
    {
        EnumSet<SXRImportSettings> settings = SXRImportSettings.getRecommendedSettingsWith(EnumSet.of(SXRImportSettings.OPTIMIZE_GRAPH, SXRImportSettings.NO_ANIMATION));
        SXRContext ctx = mAvatarRoot.getSXRContext();
        SXRResourceVolume volume = new SXRResourceVolume(ctx, avatarResource);
        SXRNode modelRoot = new SXRNode(ctx);
        SXRNode boneObject;
        int boneIndex;

        if (mSkeleton == null)
        {
            throw new IllegalArgumentException("Cannot attach model to avatar - there is no skeleton");
        }
        boneIndex = mSkeleton.getBoneIndex(attachBone);
        if (boneIndex < 0)
        {
            throw new IllegalArgumentException(attachBone + " is not a bone in the avatar skeleton");
        }
        boneObject = mSkeleton.getBone(boneIndex);
        if (boneObject == null)
        {
            throw new IllegalArgumentException(attachBone +
                    " does not have a bone object in the avatar skeleton");
        }
        boneObject.addChildObject(modelRoot);
        ctx.getAssetLoader().loadModel(volume, modelRoot, settings, false, mLoadModelHandler);
    }

    public void clearAvatar()
    {
        SXRNode previousAvatar = (mAvatarRoot.getChildrenCount() > 0) ?
                mAvatarRoot.getChildByIndex(0) : null;

        if (previousAvatar != null)
        {
            mAvatarRoot.removeChildObject(previousAvatar);
        }
    }

    /**
     * Load an animation for the current avatar.
     * @param animResource resource with the animation
     * @param boneMap optional bone map to map animation skeleton to avatar
     */
    public void loadAnimation(SXRAndroidResource animResource, String boneMap)
    {
        mBoneMap = boneMap;
        String filePath = animResource.getResourcePath();
        SXRContext ctx = mAvatarRoot.getSXRContext();
        SXRResourceVolume volume = new SXRResourceVolume(ctx, animResource);

        if (filePath.endsWith(".bvh"))
        {
            SXRAnimator animator = new SXRAnimator(ctx);
            animator.setName(filePath);
            try
            {
                BVHImporter importer = new BVHImporter(ctx);
                SXRSkeletonAnimation skelAnim;

                if (boneMap != null)
                {
                    SXRSkeleton skel = importer.importSkeleton(animResource);
                    skelAnim = importer.readMotion(skel);
                    animator.addAnimation(skelAnim);

                    SXRPoseMapper retargeter = new SXRPoseMapper(mSkeleton, skel, skelAnim.getDuration());
                    retargeter.setBoneMap(boneMap);
                    animator.addAnimation(retargeter);
                }
                else
                {
                    skelAnim = importer.importAnimation(animResource, mSkeleton);
                    animator.addAnimation(skelAnim);
                }
                addAnimation(animator);
                ctx.getEventManager().sendEvent(this,
                        IAvatarEvents.class,
                        "onAnimationLoaded",
                        SXRAvatar.this,
                        animator,
                        filePath,
                        null);
            }
            catch (IOException ex)
            {
                ctx.getEventManager().sendEvent(this,
                        IAvatarEvents.class,
                        "onAnimationLoaded",
                        SXRAvatar.this,
                        null,
                        filePath,
                        ex.getMessage());
            }
        }
        else
        {
            EnumSet<SXRImportSettings> settings = SXRImportSettings.getRecommendedSettingsWith(EnumSet.of(SXRImportSettings.OPTIMIZE_GRAPH, SXRImportSettings.NO_TEXTURING));

            SXRNode animRoot = new SXRNode(ctx);
            ctx.getAssetLoader().loadModel(volume, animRoot, settings, false, mLoadAnimHandler);
        }
    }
    /**
     * Adds an animation to this avatar.
     *
     * @param anim animation to add
     * @see SXRAvatar#removeAnimation(SXRAnimator)
     * @see SXRAvatar#clear()
     */
    public void addAnimation(SXRAnimator anim)
    {
        mAnimations.add(anim);
    }

    /**
     * Gets an animation from this avatar.
     *
     * @param index index of animation to get
     * @see SXRAvatar#addAnimation(SXRAnimator)
     */
    public SXRAnimator getAnimation(int index)
    {
        return mAnimations.get(index);
    }

    /**
     * Removes an animation from this avatar.
     *
     * @param anim animation to remove
     * @see SXRAvatar#addAnimation(SXRAnimator)
     * @see SXRAvatar#clear()
     */
    public void removeAnimation(SXRAnimator anim)
    {
        mAnimations.remove(anim);
    }

    /**
     * Removes all the animations from this avatar.
     * <p>
     * The state of the animations are not changed when removed. For example,
     * if the animations are already running they are not be stopped.
     *
     * @see SXRAvatar#removeAnimation(SXRAnimator)
     * @see SXRAvatar#addAnimation(SXRAnimator)
     */
    public void clear()
    {
        mAnimations.clear();
    }

    /**
     * Starts the named animation.
     * @see SXRAvatar#stop(String)
     * @see SXRAnimationEngine#start(SXRAnimation)
     */
    public void start(String name)
    {
        SXRAnimator anim = findAnimation(name);

        if (name.equals(anim.getName()))
        {
            start(anim);
            return;
        }
    }

    /**
     * Find the animation associated with this avatar with the given name.
     * @param name  name of animation to look for
     * @return {@link SXRAnimator} animation found or null if none with that name
     */
    public SXRAnimator findAnimation(String name)
    {
        for (SXRAnimator anim : mAnimations)
        {
            if (name.equals(anim.getName()))
            {
                return anim;
            }
        }
        return null;
    }

    /**
     * Starts the animation with the given index.
     * @param animIndex 0-based index of {@link SXRAnimator} to start;
     * @see SXRAvatar#stop()
     * @see #start(String)
     */
    public SXRAnimator start(int animIndex)
    {
        if ((animIndex < 0) || (animIndex >= mAnimations.size()))
        {
            throw new IndexOutOfBoundsException("Animation index out of bounds");
        }
        SXRAnimator anim = mAnimations.get(animIndex);
        start(anim);
        return anim;
    }

    /**
     * Start all of the avatar animations, causing them
     * to play one at a time in succession.
     * @param repeatMode SXRRepeatMode.REPEATED to repeatedly play,
     *                   SXRRepeatMode.ONCE to play only once
     *                   SXRRepeatMode.PINGPONG to play start to finish, finish to start;
     * @param repeatCount play the avatar animations as given count
     *                    -1 play repeatedly
     */
    public void startAll(int repeatMode, int repeatCount)
    {
        mRepeatMode = repeatMode;
        mRepeatCount = repeatCount;
        for (SXRAnimator anim : mAnimations)
        {
            if(mBlend && !order) {
                anim.setBlend(mBlend, mBlendFactor);
                anim.setAnimationOrder(mAnimationsOrder.FIRST);
            }
            start(anim);
        }
    }

    protected void start(SXRAnimator animator)
    {
        synchronized (mAnimQueue)
        {
            mAnimQueue.add(animator);
            if (mAnimQueue.size() > 1)
            {
                return;
            }
        }
        int x = mAnimations.size();
        mIsRunning = true;
        animator.start(mOnFinish);
        mAvatarRoot.getSXRContext().getEventManager().sendEvent(SXRAvatar.this,
                IAvatarEvents.class,
                "onAnimationStarted",
                SXRAvatar.this,
                animator);
    }

    /**
     * Evaluates the animation with the given index at the specified time.
     * @param animIndex 0-based index of {@link SXRAnimator} to start
     * @param timeInSec time to evaluate the animation at
     * @see SXRAvatar#stop()
     * @see #start(String)
     */
    public SXRAnimator animate(int animIndex, float timeInSec)
    {
        if ((animIndex < 0) || (animIndex >= mAnimations.size()))
        {
            throw new IndexOutOfBoundsException("Animation index out of bounds");
        }
        SXRAnimator anim = mAnimations.get(animIndex);
        anim.animate(timeInSec);
        return anim;
    }

    /**
     * Stops all of the animations associated with this animator.
     * @see SXRAvatar#start(String)
     * @see SXRAnimationEngine#stop(SXRAnimation)
     */
    public void stop(String name)
    {
        SXRAnimator anim = findAnimation(name);

        if (anim != null)
        {
            mIsRunning = false;
            anim.stop();
        }
    }

    /**
     * Stops the currently running animation, if any.
     * @see SXRAvatar#start(String)
     * @see SXRAnimationEngine#stop(SXRAnimation)
     */
    public void stop()
    {
        synchronized (mAnimQueue)
        {
            if (mIsRunning && (mAnimQueue.size() > 0))
            {
                mIsRunning = false;
                SXRAnimator animator = mAnimQueue.get(0);
                mAnimQueue.clear();
                animator.stop();
            }
        }
    }

    public void centerModel(SXRNode model)
    {
        SXRNode.BoundingVolume bv = model.getBoundingVolume();
        model.getTransform().setPosition(-bv.center.x, -bv.center.y, -bv.center.z - 1.5f * bv.radius);
    }

    protected IAssetEvents mLoadModelHandler = new IAssetEvents()
    {
        public void onAssetLoaded(SXRContext context, SXRNode modelRoot, String filePath, String errors)
        {
            List<SXRComponent> components = modelRoot.getAllComponents(SXRSkeleton.getComponentType());
            String eventName = "onModelLoaded";
            if ((errors != null) && !errors.isEmpty())
            {
                Log.e(TAG, "Asset load errors: " + errors);
            }
            if (components.size() > 0)
            {
                SXRSkeleton skel = (SXRSkeleton) components.get(0);
                if (mSkeleton != null)
                {
                    mSkeleton.merge(skel);
                }
                else
                {
                    mSkeleton = skel;
                    mAvatarRoot.addChildObject(modelRoot);
                    modelRoot = mAvatarRoot;
                    eventName = "onAvatarLoaded";
                }
                mSkeleton.poseFromBones();
                mSkeleton.updateSkinPose();
            }
            else
            {
                Log.e(TAG, "Avatar skeleton not found in asset file " + filePath);
            }
            context.getEventManager().sendEvent(SXRAvatar.this,
                    IAvatarEvents.class,
                    eventName,
                    SXRAvatar.this,
                    modelRoot,
                    filePath,
                    errors);
        }

        public void onModelLoaded(SXRContext context, SXRNode model, String filePath) { }
        public void onTextureLoaded(SXRContext context, SXRTexture texture, String filePath) { }
        public void onModelError(SXRContext context, String error, String filePath) { }
        public void onTextureError(SXRContext context, String error, String filePath) { }
    };

    public interface IAvatarEvents extends IEvents
    {
        public void onAvatarLoaded(SXRAvatar avatar, SXRNode avatarRoot, String filePath, String errors);
        public void onModelLoaded(SXRAvatar avatar, SXRNode avatarRoot, String filePath, String errors);
        public void onAnimationLoaded(SXRAvatar avatar, SXRAnimator animator, String filePath, String errors);
        public void onAnimationStarted(SXRAvatar avatar, SXRAnimator animator);
        public void onAnimationFinished(SXRAvatar avatar, SXRAnimator animator, SXRAnimation animation);
    }

    protected IAssetEvents mLoadAnimHandler = new IAssetEvents()
    {
        public void onAssetLoaded(SXRContext context, SXRNode animRoot, String filePath, String errors)
        {
            SXRAnimator animator = (SXRAnimator) animRoot.getComponent(SXRAnimator.getComponentType());
            if (animator == null)
            {
                if (errors == null)
                {
                    errors = "No animations found in " + filePath;
                }
                context.getEventManager().sendEvent(SXRAvatar.this,
                        IAvatarEvents.class,
                        "onAnimationLoaded",
                        SXRAvatar.this,
                        null,
                        filePath,
                        errors);
                return;
            }

            SXRSkeletonAnimation skelAnim = (SXRSkeletonAnimation) animator.getAnimation(0);
            SXRSkeleton skel = skelAnim.getSkeleton();
            if (skel != mSkeleton)
            {
                SXRPoseMapper poseMapper = new SXRPoseMapper(mSkeleton, skel, skelAnim.getDuration());

                animator.addAnimation(poseMapper);
            }
            addAnimation(animator);
            context.getEventManager().sendEvent(SXRAvatar.this,
                    IAvatarEvents.class,
                    "onAnimationLoaded",
                    SXRAvatar.this,
                    animator,
                    filePath,
                    errors);
        }

        public void onModelLoaded(SXRContext context, SXRNode model, String filePath)
        {
            centerModel(model);
        }

        public void onTextureLoaded(SXRContext context, SXRTexture texture, String filePath) { }
        public void onModelError(SXRContext context, String error, String filePath) { }
        public void onTextureError(SXRContext context, String error, String filePath) { }
    };
}