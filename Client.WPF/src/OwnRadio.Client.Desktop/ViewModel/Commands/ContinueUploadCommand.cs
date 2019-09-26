using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Input;

namespace OwnRadio.Client.Desktop.ViewModel.Commands
{
	public class ContinueUploadCommand : ICommand
	{
		public event EventHandler CanExecuteChanged;
		public ViewModelUploader Uploader { get; set; }

		public ContinueUploadCommand(ViewModelUploader uploader)
		{
			Uploader = uploader;
		}

		public bool CanExecute(object parameter)
		{
			return !Uploader.IsUploading;
		}

		public void Execute(object parameter)
		{
			//if (Uploader.UploadQueue.Count(s => !s.Uploaded) > 0)
			//	Uploader.UploadRdevFiles();
		}
	}
}
