using System;
using System.CodeDom;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Configuration;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Forms;
using Newtonsoft.Json;
using OwnRadio.Client.Desktop.Properties;
using OwnRadio.Client.Desktop.ViewModel.Commands;

namespace OwnRadio.Client.Desktop.ViewModel
{
	public class ViewModelUploader : DependencyObject
	{
		public UploadCommand UploadCommand { get; set; }
		public ContinueUploadCommand ContinueUploadCommand { get; set; }
		public HideInfoCommand HideInfoCommand { get; set; }
		public ShowInfoCommand ShowInfoCommand { get; set; }
		public ClearDbCommand ClearDbCommand { get; set; }

		public bool IsUploading
		{
			get { return (bool)GetValue(IsUploadingProperty); }
			set { SetValue(IsUploadingProperty, value); }
		}

		public static readonly DependencyProperty IsUploadingProperty =
			DependencyProperty.Register("IsUploading", typeof(bool), typeof(ViewModelUploader), new PropertyMetadata(false));

		public bool IsUploaded
		{
			get { return (bool)GetValue(IsUploadedProperty); }
			set { SetValue(IsUploadedProperty, value); }
		}
		public static readonly DependencyProperty IsUploadedProperty =
			DependencyProperty.Register("IsUploaded", typeof(bool), typeof(ViewModelUploader), new PropertyMetadata(true));

		public string Status
		{
			get { return (string)GetValue(StatusProperty); }
			set { SetValue(StatusProperty, value); }
		}

		public static readonly DependencyProperty StatusProperty =
			DependencyProperty.Register("Status", typeof(string), typeof(ViewModelUploader), new PropertyMetadata(""));

		public string Message
		{
			get { return (string)GetValue(MessageProperty); }
			set { SetValue(MessageProperty, value); }
		}
		public static readonly DependencyProperty MessageProperty =
			DependencyProperty.Register("Message", typeof(string), typeof(ViewModelUploader), new PropertyMetadata(""));

		public string Info
		{
			get { return (string)GetValue(InfoProperty); }
			set { SetValue(InfoProperty, value); }
		}
		public static readonly DependencyProperty InfoProperty =
			DependencyProperty.Register("Info", typeof(string), typeof(ViewModelUploader), new PropertyMetadata("Collapsed"));

		private readonly DataAccessLayer _dal;

		public ObservableCollection<MusicFile> UploadQueue { get; set; }

		public ViewModelUploader()
		{
			UploadCommand = new UploadCommand(this);
			ContinueUploadCommand = new ContinueUploadCommand(this);
			HideInfoCommand = new HideInfoCommand(this);
			ShowInfoCommand = new ShowInfoCommand(this);
			ClearDbCommand = new ClearDbCommand(this);

			try
			{
				_dal = new DataAccessLayer();
				UploadQueue = _dal.GetNotUploaded();
			}
			catch (Exception ex)
			{
				ShowMessage(ex.Message);
			}
		}

		public void ClearDatabase()
		{
			_dal.Clear();
			UploadQueue.Clear();
			System.Windows.MessageBox.Show("База данных успешно очищена!");
		}

		public void GetQueue(string path)
		{
			try
			{
				if (string.IsNullOrEmpty(path)) return;

				var filenames = new List<string>();
				GetMusicFiles(path, ref filenames);

				foreach (var file in filenames)
				{
					var info = new FileInfo(file);
					if (info.Length > Settings.Default.MaxTrackSize)
					{
						System.Windows.MessageBox.Show($"{file} exceeds maximum size - {Settings.Default.MaxTrackSize} bytes");
						continue;
					}

					var musicFile = new MusicFile
					{
						FileName = Path.GetFileName(file),
						FilePath = Path.GetDirectoryName(file),
						FileGuid = Guid.NewGuid()
					};

					if (_dal.AddToQueue(musicFile) > 0)
					{
						UploadQueue.Add(musicFile);
					}
				}
			}
			catch (Exception ex)
			{
				ShowMessage(ex.Message);
			}
		}

		private void GetMusicFiles(string sourceDirectory, ref List<string> filenames)
		{
			try
			{
				var allFiles = Directory.EnumerateFiles(sourceDirectory);
				var musicFiles = allFiles.Where(s => s.EndsWith(".mp3", true, CultureInfo.InvariantCulture));
				filenames.AddRange(musicFiles);

				var dirs = Directory.EnumerateDirectories(sourceDirectory);

				foreach (var directory in dirs)
					GetMusicFiles(directory, ref filenames);
			}
			catch (Exception ex)
			{
				ShowMessage(ex.Message);
			}
		}

		public async void UploadRdevFiles()
		{
			int queued = UploadQueue.Count(s => !s.Uploaded);
			int uploaded = 0;
			ShowMessage("Uploading..");
			Status = $"Uploaded: {uploaded}/{queued}";

			SetCurrentValue(IsUploadedProperty, false);
			SetCurrentValue(IsUploadingProperty, true);
			SetCurrentValue(InfoProperty, "Visible");

			try
			{
				//получаем токен для админа
				var httpClient = new HttpClient();
				var url = "http://localhost:5001/auth/login";
				var body = "{login:\"admin\", password: \"2128506\"}";
				var content = new StringContent(body, Encoding.UTF8, "application/json");
				var response = httpClient.PostAsync(url, content).Result;
				var userInfo = response.Content.ReadAsStringAsync().Result;
				dynamic token = JsonConvert.DeserializeObject(userInfo);
				var tokenValue = token.token;
				httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", tokenValue.ToString());

				//грузим файлы
				foreach (var musicFile in UploadQueue.Where(s => !s.Uploaded))
				{

					var fullFileName = musicFile.FilePath + "\\" + musicFile.FileName;

					var fileStream = File.OpenRead(fullFileName);
					var byteArray = new byte[fileStream.Length];
					fileStream.Read(byteArray, 0, (int)fileStream.Length);
					fileStream.Close();


					var fle = TagLib.File.Create(fullFileName);

					Guid recid = Guid.NewGuid();

					var request = new UploadFileDto()
					{
						Method = "uploadaudiofile"
					};

					var files = new List<FileDescription>()
					{
						new FileDescription()
						{
							RecId = recid,
							DeviceId = Guid.Parse("66666666-6666-6666-6666-666666666666"),
							Recdescription = "TrackUploaded",
							Mediatype = "track",
							LocalDevicePathUpload = fullFileName,
							Name = fle.Tag.Title,
							Content = Convert.ToBase64String(byteArray),
							Size = byteArray.Length / 1024,
							Artist = fle.Tag.FirstPerformer,
							Length = (int)Math.Round(fle.Properties.Duration.TotalSeconds, 0),
							Uploaduserid = Guid.Parse("66666666-6666-6666-6666-666666666666")
						}
					};

					request.Fields = new FilesItemDto() { files = files };

					string json = JsonConvert.SerializeObject(request);
					content = new StringContent(json, Encoding.UTF8, "application/json");

					url = "http://localhost:5001/api/executejs";
					// Выполняем запрос на Rdev

					var response2 = await httpClient.PostAsync(url, content);

					if (response2.StatusCode != HttpStatusCode.OK)
						//throw new Exception(await response.Content.ReadAsStringAsync());
						continue;

					_dal.MarkAsUploaded(musicFile);
					Status = $"Uploaded: {queued - UploadQueue.Count(s => !s.Uploaded)}/{queued}";
					++uploaded;
				}
				httpClient.Dispose();

				ShowMessage(uploaded > 0 ? "Files uploaded successfully" : "Empty queue");
			}
			catch (Exception ex)
			{
				ShowMessage(ex.Message);
			}

			SetCurrentValue(IsUploadedProperty, true);
			SetCurrentValue(IsUploadingProperty, false);
		}

		public struct FileDescription
		{
			public Guid RecId { get; set; }
			public Guid DeviceId { get; set; }
			public Guid Uploaduserid { get; set; }
			public string Recdescription { get; set; }
			public string Mediatype { get; set; }
			public int Chapter { get; set; }
			public string Name { get; set; }
			public Guid Ownerrecid { get; set; }
			public string LocalDevicePathUpload { get; set; }
			public string Content { get; set; }
			public int Size { get; set; }
			public string Outersource { get; set; }
			public string Artist { get; set; }
			public int Length { get; set; }

		}

		public struct FilesItemDto
		{
			public ICollection<FileDescription> files { get; set; }
		}

		public class UploadFileDto
		{
			public string Method { get; set; }
			public FilesItemDto Fields { get; set; }
		}

		public async void UploadFiles()
		{
			int queued = UploadQueue.Count(s => !s.Uploaded);
			int uploaded = 0;
			ShowMessage("Uploading..");
			Status = $"Uploaded: {uploaded}/{queued}";

			SetCurrentValue(IsUploadedProperty, false);
			SetCurrentValue(IsUploadingProperty, true);
			SetCurrentValue(InfoProperty, "Visible");

			try
			{
				foreach (var musicFile in UploadQueue.Where(s => !s.Uploaded))
				{
					if (await IsExist(musicFile.FileGuid))
					{
						_dal.MarkAsUploaded(musicFile);
						Status = $"Uploaded: {queued - UploadQueue.Count(s => !s.Uploaded)}/{queued}";
						++uploaded;
						continue;
					}

					var fullFileName = musicFile.FilePath + "\\" + musicFile.FileName;

					var fileStream = File.OpenRead(fullFileName);
					var byteArray = new byte[fileStream.Length];
					fileStream.Read(byteArray, 0, (int)fileStream.Length);
					fileStream.Close();

					var httpClient = new HttpClient();
					var form = new MultipartFormDataContent
					{
						{new StringContent(musicFile.FileGuid.ToString()), "fileGuid"},
						{new StringContent(fullFileName), "filePath"},
						{new StringContent(Settings.Default.DeviceId.ToString()), "deviceId"},
						{new ByteArrayContent(byteArray, 0, byteArray.Count()), "musicFile", musicFile.FileGuid + ".mp3"}
					};
					
					var response = await httpClient.PostAsync($"{Settings.Default.ServiceUri}v3/tracks", form);

					response.EnsureSuccessStatusCode();
					httpClient.Dispose();

					_dal.MarkAsUploaded(musicFile);
					Status = $"Uploaded: {queued - UploadQueue.Count(s => !s.Uploaded)}/{queued}";
					++uploaded;
				}

				ShowMessage(uploaded > 0 ? "Files uploaded successfully" : "Empty queue");
			}
			catch (Exception ex)
			{
				ShowMessage(ex.Message);
			}

			SetCurrentValue(IsUploadedProperty, true);
			SetCurrentValue(IsUploadingProperty, false);
		}

		public void Upload()
		{
			SetCurrentValue(InfoProperty, "Visible");
			var dialog = new FolderBrowserDialog();
			if (dialog.ShowDialog() != DialogResult.OK)
			{
				IsUploading = false;
				IsUploaded = true;
				return;
			}

			GetQueue(dialog.SelectedPath);
			//UploadFiles();
			UploadRdevFiles();
		}

		

		private async Task<bool> IsExist(Guid guid)
		{
			var httpClient = new HttpClient();

			var response = await httpClient.GetAsync($"{Settings.Default.ServiceUri}v3/tracks/{guid}");

			return response.IsSuccessStatusCode;
		}

		private void ShowMessage(string message)
		{
			Message = message;
		}
	}
}
