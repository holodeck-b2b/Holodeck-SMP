/*
* This file provides a password checker that will indicate the strength of the entered password and check if the
* repeated entries match. 
*/
IDS = {
	passwGrp: '.password-group',
	passwInput: '#password-input',
	passwConfirm: '#password-confirm',
	visibilityBtn: '#visibility-btn',
	strengthBar: '#password-strength-bar',
};


const testPassw = passw => {
	let strength = 'none';

	const moderate = /(?=.{8,}).*/;
	const strong = /(?=.*[A-Z])(?=.*[a-z])(?=.*[\d]).{10,}|(?=.*[\!@#$%^&*()\\[\]{}\-_+=~`|:;"'<>,./?])(?=.*[a-z])(?=.*[\d]).{10,}/;
	const veryStrong = /(?=.*[A-Z])(?=.*[a-z])(?=.*[\d])(?=.*[\!@#$%^&*()\\[\]{}\-_+=~`|:;"'<>,./?]).{20,}/;

	if (veryStrong.test(passw)) {
		strength = 'very';
	} else if (strong.test(passw)) {
		strength = 'strong';
	} else if (moderate.test(passw)) {
		strength = 'moderate';
	} else if (passw.length > 0) {
		strength = 'weak';
	}

	return strength;
};

const setStrengthBarValue = (bar, strength) => {
	let strengthValue;

	switch (strength) {
		case 'weak':
			strengthValue = 25;
			bar.setAttribute('aria-valuenow', strengthValue);
			break;
		case 'moderate':
			strengthValue = 50;
			bar.setAttribute('aria-valuenow', strengthValue);
			break;
		case 'strong':
			strengthValue = 75;
			bar.setAttribute('aria-valuenow', strengthValue);
			break;
		case 'very':
			strengthValue = 100;
			bar.setAttribute('aria-valuenow', strengthValue);
			break;
		default:
			strengthValue = 0;
			bar.setAttribute('aria-valuenow', 0);
	}
	
	return strengthValue;
};

//also adds a text label based on styles
const setStrengthBarStyles = (bar, strengthValue) => {
	bar.style.width = `${strengthValue}%`;
	bar.classList.remove('bg-success', 'bg-info', 'bg-warning');

	switch (strengthValue) {
		case 25:
			bar.classList.add('bg-danger');
			bar.textContent = 'Weak';
			break;
		case 50:
			bar.classList.remove('bg-danger');
			bar.classList.add('bg-warning');
			bar.textContent = 'Moderate';
			break;
		case 75:
			bar.classList.remove('bg-danger');
			bar.classList.add('bg-info');
			bar.textContent = 'Strong';
			break;
		case 100:
			bar.classList.remove('bg-danger');
			bar.classList.add('bg-success');
			bar.textContent = 'Very Strong';
			break;
		default:
			bar.classList.add('bg-danger');
			bar.textContent = '';
			bar.style.width = `0`;
	}
};

const passwordStrength = (input, strengthBar) => {
	//finding strength
	const strength = testPassw(input.value);

	// set validity to false if weak password
	if (strength === 'weak')
		input.setCustomValidity('Weak password is not accepted');
	else
		input.setCustomValidity('');
			
	//setting strength bar (value and styles)
	const strengthValue = setStrengthBarValue(strengthBar, strength);
	setStrengthBarStyles(strengthBar, strengthValue);
};

const passwordCompare = (confirmField, pwdField) => {
	const nextEl = confirmField.nextElementSibling;	
	const hasFbEl = nextEl && nextEl.classList.contains('invalid-feedback');
	if (confirmField.value !== pwdField.value) {
		confirmField.setCustomValidity('Entered passwords do not match');
		if (hasFbEl) 
			nextEl.textContent = 'Entered passwords do not match';
	} else {
		confirmField.setCustomValidity('');
		if (hasFbEl) 
			nextEl.textContent = 'Please provide a password for the user';
	}
};

const pwdVisibilitySwitcher = (pg) => {
	const passwField = pg.querySelector(IDS.passwInput);
	const pwdConfField = pg.querySelector(IDS.passwConfirm);
	const btn = pg.querySelector(IDS.visibilityBtn);

	if (passwField.getAttribute('type') === 'text') {
		// Switch to hidden
		passwField.setAttribute('type', 'password');
		pwdConfField.setAttribute('type', 'password');
		btn.classList.remove('bi-eye');
		btn.classList.add('bi-eye-slash');		
	} else {
		// Switch to clear text
		passwField.setAttribute('type', 'text');
		pwdConfField.setAttribute('type', 'text');
		btn.classList.remove('bi-eye-slash');
		btn.classList.add('bi-eye');		
	}
};

// Register event listeners
//
const pwdGrps = document.querySelectorAll(IDS.passwGrp);
pwdGrps.forEach(g => {
	const pwdField = g.querySelector(IDS.passwInput);
	const confirmField = g.querySelector(IDS.passwConfirm);
	const strengthBar = g.querySelector(IDS.strengthBar);
	const clientValidation = pwdField.form.classList.contains('needs-validation');
	
	pwdField.addEventListener('input', () => {
		passwordStrength(pwdField, strengthBar);
		if (clientValidation)
			passwordCompare(confirmField, pwdField);
	});

	const visbilityBtn = g.querySelector(IDS.visibilityBtn);
	visbilityBtn.addEventListener('click', e => {
		pwdVisibilitySwitcher(g);
	});
	
	if (clientValidation)
		confirmField.addEventListener('input', () => {
			passwordCompare(confirmField, pwdField);
		});
});

