import numpy as np
import cv2
from skimage import transform as trans


arcface_ref_points = np.array(
    [
        [30.2946, 51.6963],
        [65.5318, 51.5014],
        [48.0252, 71.7366],
        [33.5493, 92.3655],
        [62.7299, 92.2041],
    ],
    dtype=np.float32,
)

# lmk is prediction; src is template
def estimate_norm(lmk, image_size=112,):
    """estimate the transformation matrix
    :param lmk: detected landmarks
    :param image_size: resulting image size (default=112)
    :param createEvalDB: (boolean) crop an evaluation or training dataset
    :return: transformation matrix M and index
    """
    assert lmk.shape == (5, 2)
    assert image_size == 112
    tform = trans.SimilarityTransform()
    lmk_tran = np.insert(lmk, 2, values=np.ones(5), axis=1)
    min_M = []
    min_index = []
    min_error = float("inf")
    src = arcface_ref_points
    src = np.expand_dims(src, axis=0)

    for i in np.arange(src.shape[0]):
        tform.estimate(lmk, src[i])
        M = tform.params[0:2, :]
        results = np.dot(M, lmk_tran.T)
        results = results.T
        error = np.sum(np.sqrt(np.sum((results - src[i]) ** 2, axis=1)))
        #         print(error)
        if error < min_error:
            min_error = error
            min_M = M
            min_index = i
    return min_M, min_index


# norm_crop from Arcface repository (insightface/recognition/common/face_align.py)
def norm_crop(img, landmark, image_size=112):
    """transforms image to match the landmarks with reference landmarks
    :param landmark: detected landmarks
    :param image_size: resulting image size (default=112)
    :param createEvalDB: (boolean) crop an evaluation or training dataset
    :return: transformed image
    """
    M, pose_index = estimate_norm(landmark, image_size=image_size)
    warped = cv2.warpAffine(img, M, (image_size, image_size), borderValue=0.0)
    return warped