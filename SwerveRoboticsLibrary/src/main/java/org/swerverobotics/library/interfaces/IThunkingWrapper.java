package org.swerverobotics.library.interfaces;

/**
 * IThunkingWrapper is can be used on thunking objects to retrieve the the object
 * to which the wrapper targets its thunks.
 */
public interface IThunkingWrapper<T>
    {
    T getThunkTarget();
    }