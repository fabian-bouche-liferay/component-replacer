package com.liferay.samples.fbo.component.replacer;

import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ModelListener;
import com.liferay.portal.kernel.model.User;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(
		enabled = false,
		immediate = true,
		service = ModelListener.class
		)
public class UserModelListener extends BaseModelListener<User> {

	@Override
	public void onAfterRemove(User user) throws ModelListenerException {
		if (_log.isDebugEnabled()) {
			_log.debug(
				"NOT Removing chat entries and status for user " +
					user.getUserId());
		}
	}

	@Activate
	public void activate() {
		if (_log.isDebugEnabled()) {
			_log.debug("Activating com.liferay.samples.fbo.component.replacer.UserModelListener");
		}
	}
	
	private static final Log _log = LogFactoryUtil.getLog(
		UserModelListener.class);
	
}