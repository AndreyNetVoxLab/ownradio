//
//  LoginViewController.swift
//  OwnRadio
//
//  Created by Alexandr Serov on 13.05.2019.
//  Copyright © 2019 Netvox Lab. All rights reserved.
//

import UIKit
import GoogleSignIn

class LoginViewController: UIViewController, GIDSignInUIDelegate {

    override func viewDidLoad() {
        super.viewDidLoad()
		GIDSignIn.sharedInstance()?.uiDelegate = self
        // Do any additional setup after loading the view.
    }
	
	func sign(inWillDispatch signIn: GIDSignIn!, error: Error!) {
		if let error = error{
			print(error.localizedDescription)
		}else{
			if let user = GIDSignIn.sharedInstance()?.currentUser{
				let idToken = user.authentication.idToken ?? ""
				let givenName = user.profile.givenName ?? ""
				let email = user.profile.email ?? ""
				
				UserDefaults.standard.set(idToken, forKey: "googleToken")
				UserDefaults.standard.set(givenName, forKey: "googleUserName")
				UserDefaults.standard.set(email, forKey: "googleEmail")
			}
		}
	}
	
	
	
	func sign(_ signIn: GIDSignIn!, present viewController: UIViewController!) {
		print("signed")
	}
	
	func sign(_ signIn: GIDSignIn!, dismiss viewController: UIViewController!) {
		print(GIDSignIn.sharedInstance()?.currentUser.userID)
	}
    
    @IBAction func tapWithoutAuthorization(_ sender: Any) {
		DispatchQueue.main.async {
			let storyboard: UIStoryboard = UIStoryboard(name: "Main", bundle: nil)
			let viewController = storyboard.instantiateViewController(withIdentifier: "RadioViewController")
			self.navigationController?.pushViewController(viewController, animated: false)
		}
    }
    
    
    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

}
