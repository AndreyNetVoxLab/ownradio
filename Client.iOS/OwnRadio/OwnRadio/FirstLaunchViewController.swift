//
//  FirstLaunchViewController.swift
//  OwnRadio
//
//  Created by Alexandr Serov on 21.03.2019.
//  Copyright © 2019 Netvox Lab. All rights reserved.
//

import UIKit
import Alamofire

class FirstLaunchViewController: UIPageViewController, UIPageViewControllerDelegate, UIPageViewControllerDataSource {
	
	

	var timerAutoSkip: DispatchSourceTimer!
	var timerRunPlayer: DispatchSourceTimer!
	var reachability = NetworkReachabilityManager(host: "http://rdev.ownradio.ru/api/executejs")
	
	var pageControl = UIPageControl()
	lazy var slides:[UIViewController] = {
		return [self.newViewController(viewController: "firstSlide"),
				self.newViewController(viewController: "secondSlide"),
				self.newViewController(viewController: "thirdSlide")]
	}()
	
    override func viewDidLoad() {
        super.viewDidLoad()

		self.dataSource = self
		self.delegate = self
        // Do any additional setup after loading the view.
		
		if let firstViewController = slides.first{
			setViewControllers([firstViewController], direction: .forward, animated: true, completion: nil)
		}
		configurePageControl()
		reachability?.listener = {[unowned self] status in
			if status != NetworkReachabilityManager.NetworkReachabilityStatus.notReachable{
				self.downloadTracks()
			}
		}
		reachability?.startListening()
    }
    

	func newViewController(viewController: String) -> UIViewController{
		return UIStoryboard(name: "Main", bundle: nil).instantiateViewController(withIdentifier: viewController)
	}
	
	func configurePageControl(){
		pageControl = UIPageControl(frame: CGRect(x: 0, y: UIScreen.main.bounds.maxY - 50, width: UIScreen.main.bounds.width, height: 50))
		self.pageControl.numberOfPages = slides.count
		self.pageControl.currentPage = 0
		self.pageControl.tintColor = UIColor.black
		self.pageControl.pageIndicatorTintColor = UIColor(red: 0.0, green: 0.76, blue: 1.00, alpha: 1.0)
		self.pageControl.currentPageIndicatorTintColor = UIColor.black
		self.view.addSubview(pageControl)
	}
	
	
	func runLoginView() {
		let queue = DispatchQueue(label: "AutoSkipTimer", attributes: .concurrent)
		timerRunPlayer = DispatchSource.makeTimerSource(queue: queue)
		timerRunPlayer.scheduleOneshot(deadline: .now() + 1)
		timerRunPlayer.setEventHandler(handler: {
			DispatchQueue.main.async {
				let storyboard: UIStoryboard = UIStoryboard(name: "Main", bundle: nil)
				let viewController = storyboard.instantiateViewController(withIdentifier: "LoginViewController")
				self.navigationController?.pushViewController(viewController, animated: false)
			}
		})
		timerRunPlayer.resume()
	}
	
	override func viewDidAppear(_ animated: Bool) {
		if timerAutoSkip != nil{
			timerAutoSkip.cancel()
		}
		runAutoSkip()
	}
	
	override func viewDidDisappear(_ animated: Bool) {
		if timerAutoSkip != nil {
			timerAutoSkip.cancel()
		}
	}
	
	func pageViewController(_ pageViewController: UIPageViewController, didFinishAnimating finished: Bool, previousViewControllers: [UIViewController], transitionCompleted completed: Bool) {
		let pageContentViewController = pageViewController.viewControllers![0]
		self.pageControl.currentPage = slides.index(of: pageContentViewController)!
		
		if timerRunPlayer != nil {
			timerRunPlayer.cancel()
		}
		
		if self.pageControl.currentPage == slides.count - 1 {
			self.runLoginView()
		}
		if timerAutoSkip != nil {
			timerAutoSkip.cancel()
			runAutoSkip()
		}
	}
	
	func runAutoSkip(){
		let queue = DispatchQueue(label: "AutoSkipTimer", attributes: .concurrent)
		timerAutoSkip = DispatchSource.makeTimerSource(queue: queue)
		timerAutoSkip.scheduleRepeating(deadline: .now(), interval: .seconds(10))
		timerAutoSkip.setEventHandler(handler: {
			DispatchQueue.main.sync {
				self.goToNextPage(animated: true)
				if self.pageControl.currentPage == self.slides.count - 1{
					self.pageControl.currentPage = 0
					self.runLoginView()
				}else{
					self.pageControl.currentPage = self.pageControl.currentPage + 1
				}
			}
		})
		timerAutoSkip.resume()
	}
	
	//Перелистывание страницы
	func goToNextPage(animated: Bool){
		guard let currentViewController = self.viewControllers?.first else {return}
		guard let nextViewController = dataSource?.pageViewController(self, viewControllerAfter: currentViewController) else {return}
		self.setViewControllers([nextViewController], direction: .forward, animated: animated, completion: nil)
	}
	
	func pageViewController(_ pageViewController: UIPageViewController, viewControllerBefore viewController: UIViewController) -> UIViewController? {
		guard let viewControllerIndex = slides.index(of: viewController) else {
			return nil
		}
		
		let previousIndex = viewControllerIndex - 1
		
		guard previousIndex >= 0 else {
			return slides.last
		}
		
		guard slides.count > previousIndex else {
			return nil
		}
		return slides[previousIndex]
	}
	
	func pageViewController(_ pageViewController: UIPageViewController, viewControllerAfter viewController: UIViewController) -> UIViewController? {
		guard let viewControllerIndex = slides.index(of: viewController) else {
			return nil
		}
		
		let nextIndex = viewControllerIndex + 1
		let slidesCount = slides.count
		
		guard slidesCount != nextIndex else {
			return slides.first
		}
		
		guard slidesCount > nextIndex else {
			return nil
		}
		
		return slides[nextIndex]
		
	}
	func downloadTracks() {
		guard currentReachabilityStatus != NSObject.ReachabilityStatus.notReachable else {
			return
		}
		DispatchQueue.global(qos: .utility).async {
			Downloader.sharedInstance.runLoad(isSelf: false, complition: {
				print("First download run")
				
			})
		}
	}


}
