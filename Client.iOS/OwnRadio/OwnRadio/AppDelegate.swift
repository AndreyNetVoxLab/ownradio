//
//  AppDelegate.swift
//  OwnRadio
//
//  Created by Roman Litoshko on 11/22/16.
//  Copyright © 2016 Roll'n'Code. All rights reserved.
//

import UIKit
import HockeySDK
import GoogleSignIn
//import AppCenter
//import AppCenterCrashes
//import AppCenterAnalytics

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate, GIDSignInDelegate{
	
	
	var window: UIWindow?
	//Задаём ориентацию экрана по умолчанию
	var orientationLock = UIInterfaceOrientationMask.portrait
	
	//с этой функции начинается загрузка приложения
	func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool {
		// Override point for customization after application launch.
		URLCache.shared.removeAllCachedResponses()
		let userDefaults = UserDefaults.standard

		userDefaults.set(false, forKey: "budState")
		userDefaults.set([Date](), forKey: "budSchedule")
		//если устройству не назначен deviceId - генерируем новый
//		if userDefaults.object(forKey: "UUIDDevice") == nil {
//			let UUID = NSUUID().uuidString.lowercased() //"17096171-1C39-4290-AE50-907D7E62F36A" //
//			userDefaults.set(UUID, forKey: "UUIDDevice")
//			userDefaults.synchronize()
//		}
		
		//Проверяем в первый ли раз было запущено приложение
		
		//Регистрируем настройки по умолчанию (не меняя имеющиеся значения, если они уже есть)
		userDefaults.register(defaults: ["maxMemorySize" : 10])
		userDefaults.register(defaults: ["isOnlyWiFi" : false])
		userDefaults.register(defaults: ["trafficOptimize" : false])
		userDefaults.register(defaults: ["authToken" : ""])
		userDefaults.register(defaults: ["deviceIdentifier" : ""])
		try? userDefaults.register(defaults: ["playingSongObject" : PropertyListEncoder().encode(SongObject())])
		userDefaults.register(defaults: ["trackPosition" : 0])
		userDefaults.register(defaults: ["getTracksRatio" : 100])
		userDefaults.register(defaults: ["closeManually" : false])
		userDefaults.register(defaults: ["isPlaying" : false])
		userDefaults.register(defaults: ["timerState" : false])
		userDefaults.register(defaults: ["setTimerDate" : 0])
		userDefaults.register(defaults: ["updateTimerDate" : 0])
		userDefaults.register(defaults: ["timerDurationSeconds" : 0])
		
		//Регистрация при отсутствии токена аутентификации
		if userDefaults.string(forKey: "authToken") == "" || userDefaults.string(forKey: "deviceIdentifier") == ""{
			RdevApiService().GetAuthToken { (registerResult) in
				RdevApiService().RegisterDevice(completion: { (_) in
				})
			}
		}
		
		//userDefaults.set("", forKey: "authToken")
//		if userDefaults.object(forKey: "isAppAlreadyLaunchedOnce") == nil {
////			ApiService.shared.registerDevice()
//			RdevApiService().RegisterDevice(){comp in
//				if comp{
//					print("deviceRegistered")
//				}
//			}
//			userDefaults.set(true, forKey: "isAppAlreadyLaunchedOnce")
//			print("Приложение запущено впервые")
//		}

		// создаем папку Tracks если ее нет
		let applicationSupportPath = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
		let tracksPath = applicationSupportPath.appendingPathComponent("Tracks")
		do {
			try FileManager.default.createDirectory(at: tracksPath, withIntermediateDirectories: true, attributes: nil)
		} catch let error as NSError {
			NSLog("Unable to create directory \(error.debugDescription)")
		}
		//проверяем была ли совершена миграция
		if userDefaults.object(forKey: "MigrationWasDoneV2") == nil
		{
			DispatchQueue.global().async {
				do{
					// получаем содержимое папки Documents
					if let tracksContents = try? FileManager.default.contentsOfDirectory(atPath: FileManager.docDir()){

						self.removeFilesFromDirectory(tracksContents: tracksContents)

					}
					if let tracksContents = try? FileManager.default.contentsOfDirectory(atPath: FileManager.docDir().appending("/Tracks")) {
						self.removeFilesFromDirectory(tracksContents: tracksContents)
					}
					//удаляем треки из базы
					CoreDataManager.instance.deleteAllTracks()
					// устанавливаем флаг о прохождении миграции
					userDefaults.set(true, forKey: "MigrationWasDoneV2")
					userDefaults.synchronize()
				}
			}
		}
		
		let hockeyManager = BITHockeyManager.shared()

		hockeyManager.configure(withIdentifier: "d84512a73d904546bd54d650b88411ed")
//		 Do some additional configuration if needed here
		hockeyManager.crashManager.crashManagerStatus = BITCrashManagerStatus.alwaysAsk

//		hockeyManager.userName = "testUser"
		hockeyManager.userID = userDefaults.string(forKey: "deviceIdentifier") ?? "errorId"
//		hockeyManager.userEmail = "test@test.com"
		hockeyManager.start()
		hockeyManager.authenticator.authenticateInstallation()

//		let appCenter = MSAppCenter.self
//		appCenter.setUserId(userDefaults.string(forKey: "deviceIdentifier") ?? "" + " 1")
//		appCenter.start("d84512a7-3d90-4546-bd54-d650b88411ed", withServices: [MSAnalytics.self, MSCrashes.self])
		
		//Init sign in
		GIDSignIn.sharedInstance()?.clientID = "400574862316-pmlndl597ssjfrebrejsuro2b1ghuncj.apps.googleusercontent.com"
		GIDSignIn.sharedInstance()?.delegate = self
		return true
	}
	
	func application(_ app: UIApplication, open url: URL, options: [UIApplicationOpenURLOptionsKey : Any] = [:]) -> Bool {
		return (GIDSignIn.sharedInstance()?.handle(url as URL?,
												   sourceApplication: options[UIApplicationOpenURLOptionsKey.sourceApplication] as? String,
												   annotation: options[UIApplicationOpenURLOptionsKey.annotation]))!
	}
	
	func sign(_ signIn: GIDSignIn!, didSignInFor user: GIDGoogleUser!, withError error: Error!) {
		if let error = error{
			print("sing error \(error.localizedDescription)")
		}
		if let user = user{
			let idToken = user.authentication.idToken
			let givenName = user.profile.givenName
			let email = user.profile.email
			
			UserDefaults.standard.set(idToken, forKey: "googleToken")
			UserDefaults.standard.set(givenName, forKey: "googleUserName")
			UserDefaults.standard.set(email, forKey: "googleEmail")
		}
	}
	
	func sign(_ signIn: GIDSignIn!, didDisconnectWith user: GIDGoogleUser!, withError error: Error!) {
		//Выход
	}

	
	func removeFilesFromDirectory (tracksContents:[String]) {
		//если в папке больше 4 файлов (3 файла Sqlite и папка Tracks) то пытаемся удалить треки
		if tracksContents.count > 1 {
			for track in tracksContents {
				// проверка для удаления только треков
				if !track.contains("sqlite") {
					let atPath = FileManager.docDir().appending("/").appending(track)
					do{
						print(atPath)
						try FileManager.default.removeItem(atPath: atPath)
						
					} catch  {
						print("error with move file reason - \(error)")
					}
				}
			}
			
			
		}
	}
	

	//задаёт ориентацию экрана
	func application(_ application: UIApplication, supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
		return self.orientationLock
	}
	
	func applicationWillResignActive(_ application: UIApplication) {
		// Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
		// Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
	}
	
	func applicationDidEnterBackground(_ application: UIApplication) {
		// Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
		// If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
		application.ignoreSnapshotOnNextApplicationLaunch() //игнорирует снапшот при следующем запуске приложения
	}

	
	func applicationWillEnterForeground(_ application: UIApplication) {
		// Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
	}
	
	func applicationDidBecomeActive(_ application: UIApplication) {
		// Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
//		if let rootController = UIApplication.shared.keyWindow?.rootViewController {
//			let navigationController = rootController as! UINavigationController
//			//получаем отображаемый в текущий момент контроллер, если это контроллер видео-слайдера - возобновляем воспроизведение видео.
//			if let startViewContr = navigationController.topViewController  as? StartVideoViewController {
//				DispatchQueue.main.asyncAfter(deadline: .now() + 1.0, execute: {
//					startViewContr.playVideoBackgroud()
//				})
//
//			}
//		}
	}
	
	func applicationWillTerminate(_ application: UIApplication) {
		// Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
		UserDefaults.standard.set(false, forKey: "timerState")
		UserDefaults.standard.set(0, forKey: "timerDurationSeconds")
		UserDefaults.standard.set(true, forKey: "closeManually")
	}

}

