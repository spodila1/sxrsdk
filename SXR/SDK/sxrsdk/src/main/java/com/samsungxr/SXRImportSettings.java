/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.samsungxr;

import java.util.EnumSet;

/**
 * Encapsulates Assimp import settings to be passed in to SXRAssetLoader.
 * Do not change these values since they must match values defined in Assimp's postprocess.h
 * 
 */
public enum SXRImportSettings {
    /**
     * Tell Importer to calculate tangents in case they are not present in imported model.
     * This is required for bump map as most models won't have tangents exported.
     */
    CALCULATE_TANGENTS(0x1),
    
    
    /**
     * Tell Importer to join vertices when possible. The least vertex elements you have present in the mesh
     * the highest the probability vertices will be joined. This setting should always be on since it will
     * reduce number of vertices sent to be processed by graphics pipeline.
     */
    JOIN_IDENTICAL_VERTICES(0x2),
    
    /**
     * Tell Importer to Triangulate mesh. Triangle meshes are most suitable for rendering performance and this
     * settings should be always on. 
     */
    TRIANGULATE(0x8),
    
    /**
     * Tell Importer to calculate hard normals in case they are not present in imported model.
     */
    CALCULATE_NORMALS(0x20),
    
    /**
     * Tell Importer to calculate smooth normals in case they are not present in imported model. 
     */
    CALCULATE_SMOOTH_NORMALS(0x40),
    
    /**
     * Tell Importer to limit bone weight. Default value is at most 4 bone weights. This will optimize rendering of
     * Skinned meshes.
     */
    LIMIT_BONE_WEIGHT(0x200),
    
    /**
     * Tell Importer to reorder vertex indices to improve for cache locality. This will be useful for scenes with 
     * medium to high polygon meshes.
     */
    IMPROVE_VERTEX_CACHE_LOCALITY(0x800),
    
    /**
     * Split meshes by primitive type.
     */
    SORTBY_PRIMITIVE_TYPE(0x8000),
    
    /**
     * Optimize meshes so number of drawcalls can be reduced. Use this with OPTIMIZE_GRAPH.
     */
    OPTIMIZE_MESHES(0x200000),
    
    /**
     * Tries to merge together nodes that don't have bones, lights, animations and cameras. This is supposed to be used with
     * OPTIMIZE_MESHES. Note that this might lose reference to node names.
     */
    OPTIMIZE_GRAPH(0x400000),

    /**
     * Causes the animations in the asset to start as soon as the asset is added to the scene.
     */
    START_ANIMATIONS(0x100000),

    /**
     * Flip UV mapping in y direction.
     */
    FLIP_UV(0x800000),

    /**
     * Do not include light sources and omit vertex normals from meshes
     */
    NO_LIGHTING(0x2000000),

    /**
     * Do not include animations and omit bone weights and indices from meshes
     */
    NO_ANIMATION(0x4000000),

    /**
     * Do not include textures and omit texture coordinates from meshes
     */
    NO_TEXTURING(0x8000000),

    /**
     * Do not include blend shapes (morphs)
     */
    NO_MORPH(0x10000000);


    private int mValue;
    
    private static EnumSet<SXRImportSettings> recommendedSettings = EnumSet.of(TRIANGULATE, FLIP_UV, JOIN_IDENTICAL_VERTICES,
            LIMIT_BONE_WEIGHT, CALCULATE_TANGENTS, SORTBY_PRIMITIVE_TYPE);
    
    private SXRImportSettings(int settings) {
        mValue = settings;
    }
    
    private int getValue() {
        return mValue;
    }
    
    /**
     * This will convert the provided enum settings to assimp bitwise format.
     * Only flags used by Assimp are processed, others are ignored.
     * It's highly recommended to use one of the predefined settings fuctions lie {@link #getRecommendedSettings() getRecommendedSettings} or
     * if you want additional settings use {@link #getRecommendedSettingsWith(EnumSet)}.
     * 
     * @param settings EnumSet of all import settings desired
     * @return flag in the assimp import format.
     */
    public static int getAssimpImportFlags(EnumSet<SXRImportSettings> settings) {
        int flags = 0;
        for (SXRImportSettings s : settings) {
            long v = s.getValue();

            if (v <= FLIP_UV.getValue())
            {
                flags |= s.getValue();
            }
        }
        flags &= ~START_ANIMATIONS.getValue();
        return flags;
    }
    
    /**
     * Return recommended settings for simple meshes. If you need bumpmapped meshes use {@link #getRecommendedBumpmapSettings()}
     * @return EnumSet of recommended settings.
     */
    public static EnumSet<SXRImportSettings> getRecommendedSettings() {
        return recommendedSettings;
    }
    
    /**
     * Provides a way to add additional settings to recommended settings so you won't have to create a EnumSet from scratch.
     * @see #getRecommendedSettings()
     * @param additionalSettings
     * @return EnumSet of recommendedSettings with additional settings added.
     */
    public static EnumSet<SXRImportSettings> getRecommendedSettingsWith(EnumSet<SXRImportSettings> additionalSettings) {
        additionalSettings.addAll(recommendedSettings);
        return additionalSettings;
    }

    /**
     * Get Recommended settings for morphed meshes.
     * These settings make sure all the blend shapes have the same
     * number of vertices.
     * @return EnumSet recommended for morphed meshes.
     */
    public static EnumSet<SXRImportSettings> getRecommendedMorphSettings() {
        EnumSet<SXRImportSettings> bumpmapSettings = EnumSet.of(TRIANGULATE, FLIP_UV, LIMIT_BONE_WEIGHT, CALCULATE_TANGENTS);
        return bumpmapSettings;
    }

    /**
     * Get Recommended settings for simple bumpmap meshes.
     * @return EnumSet recommended for bumpmapped meshes.
     */
    public static EnumSet<SXRImportSettings> getRecommendedBumpmapSettings() {
        EnumSet<SXRImportSettings> bumpmapSettings = EnumSet.of(TRIANGULATE, FLIP_UV,
                LIMIT_BONE_WEIGHT,  CALCULATE_SMOOTH_NORMALS, CALCULATE_TANGENTS);
        return bumpmapSettings;
    }
}